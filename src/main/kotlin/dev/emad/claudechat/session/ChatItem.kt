package dev.emad.claudechat.session

import kotlinx.serialization.json.JsonElement

/**
 * Elemento renderable en el transcript del chat.
 *
 * Es una sealed hierarchy para que el render (Fase 4) pueda hacer `when` exhaustivo sin
 * rama `else`, y que agregar una variante nueva rompa la compilación del render hasta
 * actualizarlo — ventaja clásica de sealed classes para UI.
 */
sealed class ChatItem {

    /**
     * Mensaje del usuario escrito en el input del plugin. Es generado por la UI
     * (no viene del stream del CLI — ver [ChatEventFolder]).
     *
     * @property text contenido del prompt del usuario
     * @property attachedFiles paths (relativos al proyecto o absolutos) de archivos
     *   adjuntados como contexto
     */
    data class UserTurn(
        val text: String,
        val attachedFiles: List<String> = emptyList(),
    ) : ChatItem()

    /**
     * Texto markdown emitido por el asistente. La UI lo renderiza con
     * `MarkdownRenderer` (Fase 4) e incluye syntax highlighting.
     *
     * @property markdown contenido crudo en formato markdown
     */
    data class AssistantText(
        val markdown: String,
    ) : ChatItem()

    /**
     * Invocación de una herramienta del CLI: Bash, Read, Edit, Grep, etc.
     * Se correlaciona con un `tool_result` posterior por [toolUseId], que actualiza
     * el [status] y completa el [resultPreview].
     *
     * @property toolUseId id único de la invocación (viene del evento CLI)
     * @property name nombre de la herramienta (ej. `Bash`, `Read`)
     * @property input payload de entrada en JSON arbitrario
     * @property status estado actual de la invocación — ver [Status]
     * @property resultPreview primeras líneas del resultado (truncadas) para mostrar
     *   en la tarjeta colapsada; `null` mientras el resultado no haya llegado
     */
    data class ToolUse(
        val toolUseId: String,
        val name: String,
        val input: JsonElement,
        val status: Status,
        val resultPreview: String? = null,
    ) : ChatItem() {

        /**
         * Ciclo de vida de una [ToolUse]:
         *  - [Pending] — recién emitida, aún sin resultado
         *  - [Running] — reservado para futura telemetría; no se usa en el MVP
         *  - [Ok] — resultado recibido sin error
         *  - [Error] — la herramienta devolvió `is_error = true`
         */
        enum class Status { Pending, Running, Ok, Error }
    }

    /**
     * Aviso del sistema — por ejemplo "sesión iniciada", "CLI versión 1.2.3", etc.
     * La UI suele renderizarlo con estilo secundario.
     *
     * @property text texto a mostrar al usuario
     */
    data class SystemNotice(
        val text: String,
    ) : ChatItem()
}
