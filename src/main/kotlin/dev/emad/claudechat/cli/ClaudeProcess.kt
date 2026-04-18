package dev.emad.claudechat.cli

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Pipeline asíncrono sobre un subproceso del CLI `claude` que habla `stream-json`.
 *
 * La clase NO lanza el subproceso por sí sola: recibe los streams ya conectados más una
 * función de terminación ([terminator]). Esto permite que los tests inyecten `ByteArray*`
 * streams sin spawnear procesos reales, manteniendo la lógica de (de)codificación aislada
 * y determinista.
 *
 * Para el caso de producción, usar [ClaudeProcess.spawn] que arma todo contra `ProcessBuilder`.
 *
 * @property stdout stream de salida del CLI (NDJSON línea a línea)
 * @property stdin stream de entrada al CLI para enviar prompts / mensajes
 * @property terminator función a invocar en [close] para terminar el subproceso subyacente
 * @property codec codec que decodifica cada línea NDJSON a [ClaudeEvent]
 */
class ClaudeProcess(
    private val stdout: InputStream,
    private val stdin: OutputStream,
    private val terminator: () -> Unit,
    private val codec: StreamJsonCodec = StreamJsonCodec(),
) : AutoCloseable {

    /**
     * Flujo perezoso de eventos decodificados desde el stdout del CLI.
     *
     * Características:
     *  - Lectura línea por línea sobre [Dispatchers.IO]
     *  - Líneas en blanco se ignoran silenciosamente
     *  - Líneas que no deserializan a [ClaudeEvent] se descartan sin lanzar — el flujo sigue
     *    (forward-compat ante `type`s desconocidos del CLI)
     *  - Al cancelarse el colector o terminar el stream, cierra el reader
     *
     * La Flow es "cold": cada colección arranca un nuevo lector del mismo stream. En
     * producción normalmente se colecta una única vez.
     */
    val events: Flow<ClaudeEvent> = channelFlow {
        val reader: java.io.BufferedReader = stdout.bufferedReader(StandardCharsets.UTF_8)
        try {
            for (line: String in reader.lineSequence()) {
                if (line.isBlank()) continue
                val event: ClaudeEvent? = try {
                    codec.decodeLine<ClaudeEvent>(line)
                } catch (_: SerializationException) {
                    null
                }
                if (event != null) sendEvent(event)
            }
        } finally {
            runCatching { reader.close() }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Envía una línea al stdin del subproceso agregando un `\n` al final y haciendo flush.
     *
     * Corre sobre [Dispatchers.IO] para no bloquear el thread del llamador.
     *
     * @param line contenido a escribir (sin newline — se agrega automáticamente)
     */
    suspend fun send(line: String) {
        withContext(Dispatchers.IO) {
            stdin.write(line.toByteArray(StandardCharsets.UTF_8))
            stdin.write('\n'.code)
            stdin.flush()
        }
    }

    /**
     * Cierra los streams y dispara la terminación del subproceso subyacente.
     * Es idempotente en el sentido de que cerrar dos veces no lanza, pero el
     * [terminator] puede correr más de una vez si el usuario llama a [close] repetido —
     * los callers deberían llamar exactamente una vez (e.g., atados a `Disposer`).
     */
    override fun close() {
        runCatching { stdin.close() }
        runCatching { stdout.close() }
        terminator()
    }

    /**
     * Azúcar para enviar al canal interno de la Flow. Existe solo para evitar confusión
     * con la función `send` pública de la clase (misma nombre, distinto propósito).
     */
    private suspend fun <T> SendChannel<T>.sendEvent(value: T) {
        send(value)
    }

    companion object {

        /**
         * Flags por defecto para invocar el CLI en modo streaming, alineados con lo que
         * espera el codec (NDJSON en ambas direcciones, `--verbose` para eventos ricos).
         */
        val DEFAULT_ARGS: List<String> = listOf(
            "-p",
            "--output-format", "stream-json",
            "--input-format", "stream-json",
            "--verbose",
        )

        /**
         * Lanza el CLI como subproceso y devuelve un [ClaudeProcess] con los streams
         * conectados. El `terminator` emite `SIGTERM` y cae a `SIGKILL` si el proceso no
         * termina en 2 segundos, evitando dejar zombies.
         *
         * @param executable ruta absoluta al binario `claude`
         * @param workingDir directorio de trabajo (típicamente el root del proyecto)
         * @param args argumentos del CLI, por defecto [DEFAULT_ARGS]
         * @return instancia lista para consumir [events] y enviar mensajes con [send]
         */
        fun spawn(
            executable: Path,
            workingDir: Path,
            args: List<String> = DEFAULT_ARGS,
        ): ClaudeProcess {
            val command: List<String> = buildList {
                add(executable.toString())
                addAll(args)
            }
            val process: Process = ProcessBuilder(command)
                .directory(workingDir.toFile())
                .redirectErrorStream(false)
                .start()
            return ClaudeProcess(
                stdout = process.inputStream,
                stdin = process.outputStream,
                terminator = {
                    process.destroy()
                    val exitedCleanly: Boolean = process.waitFor(2, TimeUnit.SECONDS)
                    if (!exitedCleanly) {
                        process.destroyForcibly()
                    }
                },
            )
        }
    }
}
