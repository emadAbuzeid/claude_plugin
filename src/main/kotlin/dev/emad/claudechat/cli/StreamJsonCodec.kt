package dev.emad.claudechat.cli

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Codec para el protocolo NDJSON (JSON delimitado por saltos de línea) que emite el CLI de
 * Claude Code con `--output-format stream-json`.
 *
 * Delega la (de)serialización en [Json] de `kotlinx.serialization` y aporta:
 *  - lectura línea a línea saltando líneas en blanco,
 *  - codificación sin salto de línea final (el consumidor agrega el separador),
 *  - configuración por defecto tolerante a campos nuevos del CLI (forward-compat).
 *
 * @property json instancia de [Json] que gobierna la serialización. Por defecto usa [DEFAULT_JSON].
 */
class StreamJsonCodec(val json: Json = DEFAULT_JSON) {

    /**
     * Deserializa una única línea de NDJSON en una instancia de [T].
     *
     * Lanza [kotlinx.serialization.SerializationException] si la línea no es un JSON válido
     * o no encaja con el shape esperado de [T].
     *
     * @param line línea NDJSON (sin salto de línea final)
     * @return instancia de [T] reconstruida desde la línea
     */
    inline fun <reified T> decodeLine(line: String): T =
        json.decodeFromString(line)

    /**
     * Decodifica un stream NDJSON completo en una secuencia perezosa de instancias de [T].
     *
     * Las líneas en blanco o solo con whitespace se ignoran silenciosamente. El procesamiento
     * es lazy: no materializa toda la entrada en memoria de golpe, lo que permite consumir
     * salidas grandes sin explotar el heap.
     *
     * @param input texto NDJSON completo (acepta separadores Unix `\n` o Windows `\r\n`)
     * @return secuencia perezosa de instancias de [T], en el mismo orden que las líneas de origen
     */
    inline fun <reified T> decodeStream(input: String): Sequence<T> =
        input.lineSequence()
            .filter { it.isNotBlank() }
            .map { line: String -> json.decodeFromString<T>(line) }

    /**
     * Serializa un valor a su representación JSON canónica en una sola línea, SIN newline final.
     *
     * El consumidor es responsable de agregar el separador `\n` antes de escribir otra línea.
     *
     * @param value valor a codificar; debe pertenecer a un tipo marcado con `@Serializable`
     * @return cadena JSON sin salto de línea al final
     */
    inline fun <reified T> encodeLine(value: T): String =
        json.encodeToString(serializer<T>(), value)

    companion object {
        /**
         * Configuración por defecto del codec:
         *  - `ignoreUnknownKeys = true` — forward-compat ante nuevos campos del CLI
         *  - `classDiscriminator = "type"` — alineado con el esquema `stream-json` de Claude
         *  - `explicitNulls = false` — omite campos nulos en la salida
         *  - `encodeDefaults = false` — no emite valores iguales al default, payloads más chicos
         */
        val DEFAULT_JSON: Json = Json {
            ignoreUnknownKeys = true
            classDiscriminator = "type"
            explicitNulls = false
            encodeDefaults = false
        }
    }
}
