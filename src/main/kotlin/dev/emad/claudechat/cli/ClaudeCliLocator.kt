package dev.emad.claudechat.cli

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Información del binario CLI de Claude detectado en el sistema.
 *
 * @property executable ruta absoluta al ejecutable (ej. `/usr/local/bin/claude`)
 * @property version versión reportada por `claude --version`, parseada a [SemVer]
 */
data class ClaudeCliInfo(
    val executable: Path,
    val version: SemVer,
)

/**
 * Resuelve el binario `claude` en el sistema y descubre su versión.
 *
 * Inyecta todas las dependencias externas (variable de entorno `PATH`, filesystem y
 * ejecución del subproceso) como lambdas para permitir tests deterministas que no tocan
 * el entorno real.
 *
 * @property pathEnv valor de la variable `PATH` a recorrer (por defecto el del proceso)
 * @property pathSeparator separador de directorios en `PATH` (`:` en Unix, `;` en Windows)
 * @property binaryNames nombres candidatos del binario — se prueban en orden por directorio
 * @property fileExists predicado que indica si una ruta existe y es ejecutable
 * @property versionRunner ejecuta `<path> --version` y devuelve la salida cruda, o `null`
 *   si el proceso falla, no existe o excede el timeout
 */
class ClaudeCliLocator(
    private val pathEnv: String = System.getenv("PATH") ?: "",
    private val pathSeparator: String = File.pathSeparator,
    private val binaryNames: List<String> = listOf("claude", "claude.cmd", "claude.exe"),
    private val fileExists: (Path) -> Boolean = { path: Path -> Files.isExecutable(path) },
    private val versionRunner: (Path) -> String? = ::defaultVersionRunner,
) {

    /**
     * Interroga un binario específico y devuelve su información si responde a `--version`.
     *
     * No verifica que el archivo exista: eso es responsabilidad del llamador (ej. el
     * locator lo chequea antes). Útil cuando el usuario configuró un path explícito en
     * las Settings del plugin.
     *
     * @param executable ruta al ejecutable a interrogar
     * @return [ClaudeCliInfo] si la salida contiene una versión reconocible, `null` si el
     *   proceso falló o la salida no tiene un semver identificable
     */
    fun probe(executable: Path): ClaudeCliInfo? {
        val output: String = versionRunner(executable) ?: return null
        val version: SemVer = SemVer.parseOrNull(output) ?: return null
        return ClaudeCliInfo(executable, version)
    }

    /**
     * Busca el binario recorriendo los directorios de `PATH` en orden y probando cada
     * nombre candidato dentro de cada directorio. Devuelve el primer match válido.
     *
     * Ignora segmentos en blanco del `PATH` (frecuentes en Windows y en entornos mal
     * configurados). Si no encuentra el binario o el `PATH` está vacío, devuelve `null`.
     *
     * @return primer [ClaudeCliInfo] encontrado en orden de `PATH` y [binaryNames],
     *   o `null` si no hay ningún binario válido
     */
    fun locateOnPath(): ClaudeCliInfo? {
        if (pathEnv.isBlank()) return null
        val directories: List<Path> = pathEnv
            .split(pathSeparator)
            .filter { segment: String -> segment.isNotBlank() }
            .map { segment: String -> Path.of(segment) }
        for (directory: Path in directories) {
            for (name: String in binaryNames) {
                val candidate: Path = directory.resolve(name)
                if (fileExists(candidate)) {
                    val info: ClaudeCliInfo? = probe(candidate)
                    if (info != null) return info
                }
            }
        }
        return null
    }

    companion object {
        /**
         * Versión mínima del CLI que el plugin soporta oficialmente. Por debajo de este
         * valor el esquema `stream-json` puede no ser compatible con el codec.
         */
        val MIN_SUPPORTED_VERSION: SemVer = SemVer(1, 0, 0)
    }
}

/**
 * Ejecuta `<executable> --version` con un timeout de 5 segundos y devuelve la salida
 * combinada (stdout + stderr).
 *
 * Devuelve `null` si el proceso no pudo iniciarse ([IOException]), si fue interrumpido
 * ([InterruptedException]) o si no terminó dentro del timeout (caso en el que además
 * se destruye el subproceso para no dejar zombies).
 *
 * @param executable ruta al binario a invocar
 * @return salida cruda del comando, o `null` ante fallos o timeout
 */
private fun defaultVersionRunner(executable: Path): String? {
    return try {
        val process: Process = ProcessBuilder(executable.toString(), "--version")
            .redirectErrorStream(true)
            .start()
        val completed: Boolean = process.waitFor(5, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
            null
        } else {
            process.inputStream.readBytes().toString(Charsets.UTF_8).trim()
        }
    } catch (_: IOException) {
        null
    } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        null
    }
}
