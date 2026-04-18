package dev.emad.claudechat.cli

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Evento emitido por el CLI de Claude Code en formato `stream-json`.
 *
 * El CLI emite un JSON por línea (NDJSON). Cada objeto lleva un campo `type` que sirve como
 * discriminador de polimorfismo, alineado con la configuración por defecto del codec
 * ([StreamJsonCodec.DEFAULT_JSON] usa `classDiscriminator = "type"`).
 *
 * Los campos no declarados se descartan silenciosamente gracias a `ignoreUnknownKeys = true`;
 * esto permite absorber cambios menores del CLI sin romper el parseo (forward-compat).
 */
@Serializable
sealed class ClaudeEvent {

    /**
     * Evento de sistema del CLI — init de sesión, arranque/respuesta de hooks, etc.
     *
     * @property subtype sub-tipo específico: `init`, `hook_started`, `hook_response`, ...
     * @property sessionId identificador UUID de la sesión (opcional en algunos subtipos)
     */
    @Serializable
    @SerialName("system")
    data class System(
        val subtype: String,
        @SerialName("session_id") val sessionId: String? = null,
    ) : ClaudeEvent()

    /**
     * Mensaje del asistente con sus bloques de contenido (texto, tool_use o thinking).
     *
     * @property message el mensaje emitido
     * @property sessionId identificador UUID de la sesión
     * @property parentToolUseId id del tool_use padre si este mensaje es resultado de uno
     *   (caso de sub-agentes o herramientas que llaman al modelo recursivamente)
     */
    @Serializable
    @SerialName("assistant")
    data class Assistant(
        val message: AssistantMessage,
        @SerialName("session_id") val sessionId: String? = null,
        @SerialName("parent_tool_use_id") val parentToolUseId: String? = null,
    ) : ClaudeEvent()

    /**
     * Mensaje del usuario: prompt inicial o contenedor de `tool_result` devueltos al modelo.
     *
     * @property message el mensaje del usuario (contenido textual o resultados de tools)
     * @property sessionId identificador UUID de la sesión
     * @property parentToolUseId id del tool_use padre si aplica
     */
    @Serializable
    @SerialName("user")
    data class User(
        val message: UserMessage,
        @SerialName("session_id") val sessionId: String? = null,
        @SerialName("parent_tool_use_id") val parentToolUseId: String? = null,
    ) : ClaudeEvent()

    /**
     * Evento terminal de una sesión. Indica que el agente terminó el turno con éxito o error
     * y aporta métricas (duración, costo, motivo de parada).
     *
     * @property subtype sub-tipo, típicamente `success` o `error`
     * @property isError `true` si la ejecución terminó en error
     * @property durationMs duración total en milisegundos
     * @property sessionId identificador UUID de la sesión
     * @property totalCostUsd costo total estimado en dólares (si está disponible)
     * @property stopReason razón declarada de parada (ej. `end_turn`)
     */
    @Serializable
    @SerialName("result")
    data class Result(
        val subtype: String,
        @SerialName("is_error") val isError: Boolean = false,
        @SerialName("duration_ms") val durationMs: Long = 0L,
        @SerialName("session_id") val sessionId: String? = null,
        @SerialName("total_cost_usd") val totalCostUsd: Double? = null,
        @SerialName("stop_reason") val stopReason: String? = null,
    ) : ClaudeEvent()

    /**
     * Evento de rate-limit emitido por el CLI cuando el backend responde con info de cuota.
     * No bloquea el flujo; la UI puede mostrarlo como notificación silenciosa.
     *
     * @property info bloque arbitrario con la metadata del rate-limit (estado, reset, etc.)
     * @property sessionId identificador UUID de la sesión
     */
    @Serializable
    @SerialName("rate_limit_event")
    data class RateLimitEvent(
        @SerialName("rate_limit_info") val info: JsonObject? = null,
        @SerialName("session_id") val sessionId: String? = null,
    ) : ClaudeEvent()
}

/**
 * Cuerpo del mensaje del asistente. Refleja la forma del objeto `message` en los eventos
 * de tipo `assistant`.
 *
 * @property id identificador único del mensaje (ej. `msg_01...`)
 * @property role rol, siempre `assistant` en la práctica
 * @property model identificador del modelo que generó el mensaje
 * @property content lista de bloques (texto, tool_use, thinking)
 * @property stopReason motivo de finalización del modelo (ej. `end_turn`, `tool_use`)
 */
@Serializable
data class AssistantMessage(
    val id: String? = null,
    val role: String = "assistant",
    val model: String? = null,
    val content: List<AssistantBlock> = emptyList(),
    @SerialName("stop_reason") val stopReason: String? = null,
)

/**
 * Cuerpo del mensaje del usuario. Puede contener texto o resultados de herramientas.
 *
 * @property role rol, siempre `user`
 * @property content lista de bloques (texto o tool_result)
 */
@Serializable
data class UserMessage(
    val role: String = "user",
    val content: List<UserBlock> = emptyList(),
)

/**
 * Un bloque dentro del contenido de un [AssistantMessage]. Polimórfico por el campo `type`.
 */
@Serializable
sealed class AssistantBlock {

    /**
     * Texto plano emitido por el asistente. En la UI se renderiza como markdown.
     *
     * @property text contenido textual; puede incluir sintaxis markdown
     */
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : AssistantBlock()

    /**
     * Invocación de una herramienta por parte del asistente. El `input` es un JSON arbitrario
     * dependiente de la herramienta (ej. `{"command":"ls -la"}` para Bash).
     *
     * @property id identificador único de la invocación (se correlaciona con [UserBlock.ToolResult.toolUseId])
     * @property name nombre de la herramienta (ej. `Bash`, `Read`, `Edit`)
     * @property input payload de entrada en formato JSON abierto
     */
    @Serializable
    @SerialName("tool_use")
    data class ToolUse(
        val id: String,
        val name: String,
        val input: JsonElement,
    ) : AssistantBlock()

    /**
     * Bloque de razonamiento interno del modelo. El contenido suele venir cifrado en el campo
     * `signature`; la UI normalmente lo oculta o lo muestra colapsado.
     *
     * @property thinking texto crudo del razonamiento (puede estar vacío)
     * @property signature firma/cifrado opaco del contenido de thinking
     */
    @Serializable
    @SerialName("thinking")
    data class Thinking(
        val thinking: String = "",
        val signature: String? = null,
    ) : AssistantBlock()
}

/**
 * Un bloque dentro del contenido de un [UserMessage]. Polimórfico por el campo `type`.
 */
@Serializable
sealed class UserBlock {

    /**
     * Texto plano del usuario. Caso típico del prompt inicial de una sesión.
     *
     * @property text contenido textual del usuario
     */
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : UserBlock()

    /**
     * Resultado de la ejecución de una herramienta, devuelto al modelo para continuar el
     * razonamiento. El campo `content` puede ser un string o una lista de sub-bloques, por
     * eso se modela como [JsonElement] y la capa superior lo interpreta.
     *
     * @property toolUseId id que referencia el [AssistantBlock.ToolUse] original
     * @property content payload crudo del resultado (string u objeto/array)
     * @property isError `true` si la herramienta devolvió un error
     */
    @Serializable
    @SerialName("tool_result")
    data class ToolResult(
        @SerialName("tool_use_id") val toolUseId: String,
        val content: JsonElement,
        @SerialName("is_error") val isError: Boolean = false,
    ) : UserBlock()
}
