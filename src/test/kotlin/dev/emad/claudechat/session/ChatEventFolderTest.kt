package dev.emad.claudechat.session

import dev.emad.claudechat.cli.AssistantBlock
import dev.emad.claudechat.cli.AssistantMessage
import dev.emad.claudechat.cli.ClaudeEvent
import dev.emad.claudechat.cli.UserBlock
import dev.emad.claudechat.cli.UserMessage
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChatEventFolderTest {

    private val folder: ChatEventFolder = ChatEventFolder()

    @Nested
    inner class AssistantEvents {

        @Test
        fun `Text block appends an AssistantText item`() {
            val event: ClaudeEvent = assistant(AssistantBlock.Text("hola mundo"))
            val items: List<ChatItem> = folder.fold(emptyList(), event)
            assertEquals(1, items.size)
            val item: ChatItem.AssistantText = assertIs<ChatItem.AssistantText>(items.single())
            assertEquals("hola mundo", item.markdown)
        }

        @Test
        fun `ToolUse block appends a ToolUse item with Pending status`() {
            val input: kotlinx.serialization.json.JsonElement = buildJsonObject { put("cmd", "ls -la") }
            val event: ClaudeEvent = assistant(
                AssistantBlock.ToolUse(id = "toolu_1", name = "Bash", input = input),
            )
            val items: List<ChatItem> = folder.fold(emptyList(), event)
            val tool: ChatItem.ToolUse = assertIs<ChatItem.ToolUse>(items.single())
            assertEquals("toolu_1", tool.toolUseId)
            assertEquals("Bash", tool.name)
            assertEquals(input, tool.input)
            assertEquals(ChatItem.ToolUse.Status.Pending, tool.status)
            assertEquals(null, tool.resultPreview)
        }

        @Test
        fun `Thinking block is skipped silently`() {
            val event: ClaudeEvent = assistant(
                AssistantBlock.Thinking(thinking = "internal chain of thought"),
            )
            val items: List<ChatItem> = folder.fold(emptyList(), event)
            assertTrue(items.isEmpty())
        }

        @Test
        fun `multiple blocks in one message produce multiple items in order`() {
            val event: ClaudeEvent = assistant(
                AssistantBlock.Text("leyendo"),
                AssistantBlock.ToolUse(id = "toolu_1", name = "Read", input = JsonPrimitive("/x")),
            )
            val items: List<ChatItem> = folder.fold(emptyList(), event)
            assertEquals(2, items.size)
            assertIs<ChatItem.AssistantText>(items[0])
            assertIs<ChatItem.ToolUse>(items[1])
        }
    }

    @Nested
    inner class ToolResultCorrelation {

        @Test
        fun `User ToolResult with is_error=false marks matching ToolUse as Ok`() {
            val pending: ChatItem.ToolUse = ChatItem.ToolUse(
                toolUseId = "toolu_42",
                name = "Bash",
                input = JsonPrimitive("ls"),
                status = ChatItem.ToolUse.Status.Pending,
            )
            val state: List<ChatItem> = listOf(pending)
            val event: ClaudeEvent = user(
                UserBlock.ToolResult(
                    toolUseId = "toolu_42",
                    content = JsonPrimitive("total 0\n"),
                    isError = false,
                ),
            )
            val items: List<ChatItem> = folder.fold(state, event)
            val tool: ChatItem.ToolUse = assertIs<ChatItem.ToolUse>(items.single())
            assertEquals(ChatItem.ToolUse.Status.Ok, tool.status)
            assertEquals("total 0", tool.resultPreview?.trim())
        }

        @Test
        fun `User ToolResult with is_error=true marks matching ToolUse as Error`() {
            val pending: ChatItem.ToolUse = ChatItem.ToolUse(
                toolUseId = "toolu_fail",
                name = "Bash",
                input = JsonPrimitive("unknowncmd"),
                status = ChatItem.ToolUse.Status.Pending,
            )
            val event: ClaudeEvent = user(
                UserBlock.ToolResult(
                    toolUseId = "toolu_fail",
                    content = JsonPrimitive("command not found"),
                    isError = true,
                ),
            )
            val items: List<ChatItem> = folder.fold(listOf(pending), event)
            val tool: ChatItem.ToolUse = assertIs<ChatItem.ToolUse>(items.single())
            assertEquals(ChatItem.ToolUse.Status.Error, tool.status)
            assertEquals("command not found", tool.resultPreview)
        }

        @Test
        fun `ToolResult without matching ToolUse leaves state unchanged`() {
            val existing: ChatItem = ChatItem.AssistantText(markdown = "hola")
            val event: ClaudeEvent = user(
                UserBlock.ToolResult(
                    toolUseId = "toolu_missing",
                    content = JsonPrimitive("irrelevant"),
                    isError = false,
                ),
            )
            val items: List<ChatItem> = folder.fold(listOf(existing), event)
            assertEquals(listOf(existing), items)
        }

        @Test
        fun `ToolResult with array content uses a JSON string preview`() {
            val pending: ChatItem.ToolUse = ChatItem.ToolUse(
                toolUseId = "toolu_arr",
                name = "Read",
                input = JsonPrimitive("/tmp"),
                status = ChatItem.ToolUse.Status.Pending,
            )
            val arrayContent: kotlinx.serialization.json.JsonElement = buildJsonArray {
                add(buildJsonObject {
                    put("type", "text")
                    put("text", "file contents")
                })
            }
            val event: ClaudeEvent = user(
                UserBlock.ToolResult(
                    toolUseId = "toolu_arr",
                    content = arrayContent,
                    isError = false,
                ),
            )
            val items: List<ChatItem> = folder.fold(listOf(pending), event)
            val tool: ChatItem.ToolUse = assertIs<ChatItem.ToolUse>(items.single())
            assertTrue(tool.resultPreview!!.contains("file contents"))
        }
    }

    @Nested
    inner class UserTextEvents {

        @Test
        fun `User Text block appends a UserTurn`() {
            val event: ClaudeEvent = user(UserBlock.Text("quiero saber la hora"))
            val items: List<ChatItem> = folder.fold(emptyList(), event)
            val turn: ChatItem.UserTurn = assertIs<ChatItem.UserTurn>(items.single())
            assertEquals("quiero saber la hora", turn.text)
        }
    }

    @Nested
    inner class NonActionableEvents {

        @Test
        fun `System event does not change the state`() {
            val event: ClaudeEvent = ClaudeEvent.System(subtype = "init", sessionId = "abc")
            val items: List<ChatItem> = folder.fold(emptyList(), event)
            assertTrue(items.isEmpty())
        }

        @Test
        fun `Result event does not change the state`() {
            val existing: ChatItem = ChatItem.AssistantText("hola")
            val event: ClaudeEvent = ClaudeEvent.Result(subtype = "success")
            val items: List<ChatItem> = folder.fold(listOf(existing), event)
            assertEquals(listOf(existing), items)
        }

        @Test
        fun `RateLimitEvent does not change the state`() {
            val existing: ChatItem = ChatItem.AssistantText("hola")
            val event: ClaudeEvent = ClaudeEvent.RateLimitEvent()
            val items: List<ChatItem> = folder.fold(listOf(existing), event)
            assertEquals(listOf(existing), items)
        }
    }

    private fun assistant(vararg blocks: AssistantBlock): ClaudeEvent.Assistant =
        ClaudeEvent.Assistant(
            message = AssistantMessage(content = blocks.toList()),
            sessionId = "test-session",
        )

    private fun user(vararg blocks: UserBlock): ClaudeEvent.User =
        ClaudeEvent.User(
            message = UserMessage(content = blocks.toList()),
            sessionId = "test-session",
        )
}
