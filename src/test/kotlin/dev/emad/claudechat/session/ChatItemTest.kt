package dev.emad.claudechat.session

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ChatItemTest {

    @Nested
    inner class Construction {

        @Test
        fun `UserTurn stores text and attached files`() {
            val turn: ChatItem.UserTurn = ChatItem.UserTurn(
                text = "hola",
                attachedFiles = listOf("src/main/App.kt"),
            )
            assertEquals("hola", turn.text)
            assertEquals(listOf("src/main/App.kt"), turn.attachedFiles)
        }

        @Test
        fun `AssistantText stores markdown`() {
            val text: ChatItem.AssistantText = ChatItem.AssistantText(markdown = "**bold**")
            assertEquals("**bold**", text.markdown)
        }

        @Test
        fun `ToolUse starts Pending with no result preview`() {
            val input: kotlinx.serialization.json.JsonElement = buildJsonObject { put("cmd", "ls") }
            val use: ChatItem.ToolUse = ChatItem.ToolUse(
                toolUseId = "toolu_1",
                name = "Bash",
                input = input,
                status = ChatItem.ToolUse.Status.Pending,
            )
            assertEquals("toolu_1", use.toolUseId)
            assertEquals("Bash", use.name)
            assertEquals(input, use.input)
            assertEquals(ChatItem.ToolUse.Status.Pending, use.status)
            assertEquals(null, use.resultPreview)
        }

        @Test
        fun `SystemNotice stores text`() {
            val notice: ChatItem.SystemNotice = ChatItem.SystemNotice(text = "sesion iniciada")
            assertEquals("sesion iniciada", notice.text)
        }
    }

    @Nested
    inner class Equality {

        @Test
        fun `data class equality holds across equal field values`() {
            val a: ChatItem = ChatItem.AssistantText("x")
            val b: ChatItem = ChatItem.AssistantText("x")
            assertEquals(a, b)
        }

        @Test
        fun `different variants never compare equal`() {
            val user: ChatItem = ChatItem.UserTurn(text = "hola")
            val assistant: ChatItem = ChatItem.AssistantText(markdown = "hola")
            assertNotEquals<ChatItem>(user, assistant)
        }

        @Test
        fun `ToolUse copy advances the status and adds the preview`() {
            val original: ChatItem.ToolUse = ChatItem.ToolUse(
                toolUseId = "toolu_1",
                name = "Read",
                input = JsonPrimitive("/tmp"),
                status = ChatItem.ToolUse.Status.Pending,
            )
            val finished: ChatItem.ToolUse = original.copy(
                status = ChatItem.ToolUse.Status.Ok,
                resultPreview = "ok",
            )
            assertEquals("toolu_1", finished.toolUseId)
            assertEquals(ChatItem.ToolUse.Status.Ok, finished.status)
            assertEquals("ok", finished.resultPreview)
            assertNotEquals(original, finished)
        }
    }
}
