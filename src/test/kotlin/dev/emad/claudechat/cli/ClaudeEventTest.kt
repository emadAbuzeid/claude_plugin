package dev.emad.claudechat.cli

import org.junit.jupiter.api.Nested
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ClaudeEventTest {

    private val codec: StreamJsonCodec = StreamJsonCodec()

    @Nested
    inner class HelloFixture {

        private val events: List<ClaudeEvent> = loadEvents("hello.ndjson")

        @Test
        fun `parses every line of the fixture without throwing`() {
            assertTrue(events.isNotEmpty(), "expected at least one event from hello.ndjson")
        }

        @Test
        fun `contains at least one System event`() {
            val systemEvents: List<ClaudeEvent.System> = events.filterIsInstance<ClaudeEvent.System>()
            assertTrue(systemEvents.isNotEmpty())
        }

        @Test
        fun `ends with a single successful Result event`() {
            val resultEvents: List<ClaudeEvent.Result> = events.filterIsInstance<ClaudeEvent.Result>()
            assertEquals(1, resultEvents.size)
            val result: ClaudeEvent.Result = resultEvents.single()
            assertEquals("success", result.subtype)
            assertEquals(false, result.isError)
        }

        @Test
        fun `contains an Assistant event whose message has at least one Text block`() {
            val assistant: ClaudeEvent.Assistant? = events
                .filterIsInstance<ClaudeEvent.Assistant>()
                .firstOrNull()
            assertNotNull(assistant)
            val textBlock: AssistantBlock.Text? = assistant.message.content
                .filterIsInstance<AssistantBlock.Text>()
                .firstOrNull()
            assertNotNull(textBlock)
            assertTrue(textBlock.text.isNotBlank())
        }

        @Test
        fun `Assistant event carries a non-blank session_id`() {
            val assistant: ClaudeEvent.Assistant = events
                .filterIsInstance<ClaudeEvent.Assistant>()
                .first()
            val sessionId: String? = assistant.sessionId
            assertNotNull(sessionId)
            assertTrue(sessionId.isNotBlank())
        }
    }

    @Nested
    inner class ToolUseFixture {

        private val events: List<ClaudeEvent> = loadEvents("tool_use_ls.ndjson")

        @Test
        fun `contains at least one ToolUse block inside an Assistant message`() {
            val toolUses: List<AssistantBlock.ToolUse> = events
                .filterIsInstance<ClaudeEvent.Assistant>()
                .flatMap { event: ClaudeEvent.Assistant -> event.message.content }
                .filterIsInstance<AssistantBlock.ToolUse>()
            assertTrue(toolUses.isNotEmpty())
        }

        @Test
        fun `each ToolUse has an id and a non-blank name`() {
            val toolUses: List<AssistantBlock.ToolUse> = events
                .filterIsInstance<ClaudeEvent.Assistant>()
                .flatMap { event: ClaudeEvent.Assistant -> event.message.content }
                .filterIsInstance<AssistantBlock.ToolUse>()
            toolUses.forEach { use: AssistantBlock.ToolUse ->
                assertTrue(use.id.isNotBlank(), "tool_use id must be present")
                assertTrue(use.name.isNotBlank(), "tool_use name must be present")
            }
        }

        @Test
        fun `contains at least one ToolResult block inside a User message`() {
            val toolResults: List<UserBlock.ToolResult> = events
                .filterIsInstance<ClaudeEvent.User>()
                .flatMap { event: ClaudeEvent.User -> event.message.content }
                .filterIsInstance<UserBlock.ToolResult>()
            assertTrue(toolResults.isNotEmpty())
        }

        @Test
        fun `each ToolResult references a non-blank tool_use_id`() {
            val toolResults: List<UserBlock.ToolResult> = events
                .filterIsInstance<ClaudeEvent.User>()
                .flatMap { event: ClaudeEvent.User -> event.message.content }
                .filterIsInstance<UserBlock.ToolResult>()
            toolResults.forEach { result: UserBlock.ToolResult ->
                assertTrue(result.toolUseId.isNotBlank())
            }
        }
    }

    @Nested
    inner class ReadToolFixture {

        private val events: List<ClaudeEvent> = loadEvents("tool_use_read.ndjson")

        @Test
        fun `stream terminates with a Result event`() {
            assertIs<ClaudeEvent.Result>(events.last())
        }

        @Test
        fun `contains an Assistant event carrying a model identifier`() {
            val assistant: ClaudeEvent.Assistant = events
                .filterIsInstance<ClaudeEvent.Assistant>()
                .first()
            val model: String? = assistant.message.model
            assertNotNull(model)
            assertTrue(model.isNotBlank())
        }
    }

    private fun loadEvents(fixtureName: String): List<ClaudeEvent> {
        val resource: URL = javaClass.getResource("/fixtures/$fixtureName")
            ?: error(
                "Fixture $fixtureName missing. Run the CLI capture commands documented in the README " +
                    "(Fase 2) to regenerate src/test/resources/fixtures/.",
            )
        val raw: String = resource.readText()
        return codec.decodeStream<ClaudeEvent>(raw).toList()
    }
}
