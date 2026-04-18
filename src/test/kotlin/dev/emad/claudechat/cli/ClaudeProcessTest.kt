package dev.emad.claudechat.cli

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ClaudeProcessTest {

    @Nested
    inner class Events {

        @Test
        fun `emits ClaudeEvent instances from a real NDJSON fixture`(): Unit = runBlocking {
            val fixture: String = readFixture("hello.ndjson")
            val process: ClaudeProcess = ClaudeProcess(
                stdout = fixture.byteInputStream(),
                stdin = ByteArrayOutputStream(),
                terminator = { },
            )
            val events: List<ClaudeEvent> = process.events.toList()
            assertTrue(events.isNotEmpty())
            assertIs<ClaudeEvent.Result>(events.last())
        }

        @Test
        fun `skips malformed JSON lines without crashing the stream`(): Unit = runBlocking {
            val stdout: String = """
                {"type":"system","subtype":"init","session_id":"abc"}
                this is not valid json
                {"type":"result","subtype":"success","is_error":false}
            """.trimIndent()
            val process: ClaudeProcess = ClaudeProcess(
                stdout = stdout.byteInputStream(),
                stdin = ByteArrayOutputStream(),
                terminator = { },
            )
            val events: List<ClaudeEvent> = process.events.toList()
            assertEquals(2, events.size, "expected malformed line to be skipped")
            assertIs<ClaudeEvent.System>(events.first())
            assertIs<ClaudeEvent.Result>(events.last())
        }

        @Test
        fun `ignores blank lines in stdout`(): Unit = runBlocking {
            val stdout: String = "\n\n{\"type\":\"system\",\"subtype\":\"init\"}\n\n"
            val process: ClaudeProcess = ClaudeProcess(
                stdout = stdout.byteInputStream(),
                stdin = ByteArrayOutputStream(),
                terminator = { },
            )
            val events: List<ClaudeEvent> = process.events.toList()
            assertEquals(1, events.size)
        }
    }

    @Nested
    inner class Send {

        @Test
        fun `writes the line followed by newline to stdin`(): Unit = runBlocking {
            val captured: ByteArrayOutputStream = ByteArrayOutputStream()
            val process: ClaudeProcess = ClaudeProcess(
                stdout = emptyStdout(),
                stdin = captured,
                terminator = { },
            )
            process.send("hola")
            assertEquals("hola\n", captured.toByteArray().toString(Charsets.UTF_8))
        }

        @Test
        fun `sends multiple lines preserving order`(): Unit = runBlocking {
            val captured: ByteArrayOutputStream = ByteArrayOutputStream()
            val process: ClaudeProcess = ClaudeProcess(
                stdout = emptyStdout(),
                stdin = captured,
                terminator = { },
            )
            process.send("primera")
            process.send("segunda")
            assertEquals("primera\nsegunda\n", captured.toByteArray().toString(Charsets.UTF_8))
        }
    }

    @Nested
    inner class Close {

        @Test
        fun `invokes the terminator exactly once`() {
            var terminatorCalls: Int = 0
            val process: ClaudeProcess = ClaudeProcess(
                stdout = emptyStdout(),
                stdin = ByteArrayOutputStream(),
                terminator = { terminatorCalls += 1 },
            )
            process.close()
            assertEquals(1, terminatorCalls)
        }
    }

    private fun emptyStdout(): InputStream = ByteArrayInputStream(ByteArray(0))

    private fun readFixture(name: String): String {
        val resource: URL = javaClass.getResource("/fixtures/$name")
            ?: error(
                "Fixture $name missing. Regenerate following src/test/resources/fixtures/README.md.",
            )
        return resource.readText()
    }
}
