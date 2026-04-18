package dev.emad.claudechat.session

import dev.emad.claudechat.cli.AssistantBlock
import dev.emad.claudechat.cli.ClaudeEvent
import dev.emad.claudechat.cli.UserBlock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Transforma eventos crudos del CLI ([ClaudeEvent]) en actualizaciones incrementales del
 * transcript ([ChatItem]).
 *
 * Es una función **pura** por diseño: dado un estado y un evento, devuelve el nuevo estado
 * sin efectos secundarios. Esto permite testear el mapeo sin setup de coroutines ni
 * procesos reales, y simplifica la lógica de la capa superior ([ChatSession]) a un
 * `scan` sobre la Flow.
 *
 * @property maxPreviewChars tamaño máximo del preview de un `tool_result` (truncación
 *   centrada con elipsis si excede)
 */
class ChatEventFolder(
    private val maxPreviewChars: Int = 400,
) {

    /**
     * Devuelve una nueva lista de [ChatItem] aplicando el evento al estado actual.
     *
     * Mapeo de eventos a mutaciones:
     *  - `Assistant` con bloque `Text` → appende [ChatItem.AssistantText]
     *  - `Assistant` con bloque `ToolUse` → appende [ChatItem.ToolUse] en `Pending`
     *  - `Assistant` con bloque `Thinking` → sin cambios (el razonamiento interno no va al UI)
     *  - `User` con bloque `Text` → appende [ChatItem.UserTurn]
     *  - `User` con bloque `ToolResult` → actualiza el [ChatItem.ToolUse] con el mismo
     *    `tool_use_id` a `Ok` o `Error` con preview del contenido; si no hay match, ignora
     *  - `System` / `Result` / `RateLimitEvent` → sin cambios (el MVP no los muestra)
     *
     * @param current estado actual del transcript (inmutable)
     * @param event evento recién llegado del stream del CLI
     * @return nuevo estado del transcript (también inmutable)
     */
    fun fold(current: List<ChatItem>, event: ClaudeEvent): List<ChatItem> = when (event) {
        is ClaudeEvent.Assistant -> event.message.content.fold(current, ::applyAssistantBlock)
        is ClaudeEvent.User -> event.message.content.fold(current, ::applyUserBlock)
        is ClaudeEvent.System -> current
        is ClaudeEvent.Result -> current
        is ClaudeEvent.RateLimitEvent -> current
    }

    /**
     * Aplica un bloque de un mensaje del asistente al estado.
     */
    private fun applyAssistantBlock(state: List<ChatItem>, block: AssistantBlock): List<ChatItem> =
        when (block) {
            is AssistantBlock.Text -> state + ChatItem.AssistantText(markdown = block.text)
            is AssistantBlock.ToolUse -> state + ChatItem.ToolUse(
                toolUseId = block.id,
                name = block.name,
                input = block.input,
                status = ChatItem.ToolUse.Status.Pending,
            )
            is AssistantBlock.Thinking -> state
        }

    /**
     * Aplica un bloque de un mensaje del usuario al estado. En el caso de [UserBlock.ToolResult]
     * busca la [ChatItem.ToolUse] correspondiente y la actualiza en el lugar.
     */
    private fun applyUserBlock(state: List<ChatItem>, block: UserBlock): List<ChatItem> =
        when (block) {
            is UserBlock.Text -> state + ChatItem.UserTurn(text = block.text)
            is UserBlock.ToolResult -> state.map { item: ChatItem ->
                if (item is ChatItem.ToolUse && item.toolUseId == block.toolUseId) {
                    item.copy(
                        status = if (block.isError) ChatItem.ToolUse.Status.Error else ChatItem.ToolUse.Status.Ok,
                        resultPreview = previewOf(block.content),
                    )
                } else {
                    item
                }
            }
        }

    /**
     * Convierte el contenido de un `tool_result` a un preview textual truncado.
     *
     * Si el contenido es un string JSON, devuelve ese string (truncado). Si es un array u
     * objeto (como el shape con bloques `text`), devuelve su representación JSON compacta.
     *
     * @param element contenido del `tool_result`
     * @return preview textual para mostrar en la tarjeta colapsable
     */
    private fun previewOf(element: JsonElement): String {
        val raw: String = when {
            element is JsonPrimitive && element.isString -> element.content
            else -> element.toString()
        }
        return if (raw.length <= maxPreviewChars) {
            raw
        } else {
            val half: Int = maxPreviewChars / 2
            raw.take(half) + "…" + raw.takeLast(half)
        }
    }
}
