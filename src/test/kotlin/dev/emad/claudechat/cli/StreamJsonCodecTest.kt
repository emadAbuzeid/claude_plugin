package dev.emad.claudechat.cli

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StreamJsonCodecTest {

    @Serializable
    private data class Probe(val name: String, val n: Int)

    private val codec: StreamJsonCodec = StreamJsonCodec()

    @Nested
    inner class DecodeLine {

        @Test
        fun `decodes a well-formed single JSON line`() {
            val line: String = """{"name":"x","n":1}"""
            val decoded: Probe = codec.decodeLine(line)
            assertEquals(Probe("x", 1), decoded)
        }

        @Test
        fun `ignores unknown fields for forward compatibility`() {
            val line: String = """{"name":"x","n":1,"extra":"ignored"}"""
            val decoded: Probe = codec.decodeLine(line)
            assertEquals(Probe("x", 1), decoded)
        }

        @Test
        fun `throws SerializationException on malformed JSON`() {
            val line: String = """{"name":"x","n":}"""
            assertFailsWith<SerializationException> { codec.decodeLine<Probe>(line) }
        }
    }

    @Nested
    inner class DecodeStream {

        @Test
        fun `decodes multiple NDJSON lines in order`() {
            val stream: String = """
                {"name":"a","n":1}
                {"name":"b","n":2}
                {"name":"c","n":3}
            """.trimIndent()
            val decoded: List<Probe> = codec.decodeStream<Probe>(stream).toList()
            assertEquals(
                listOf(Probe("a", 1), Probe("b", 2), Probe("c", 3)),
                decoded,
            )
        }

        @Test
        fun `skips blank lines between events`() {
            val stream: String = "{\"name\":\"a\",\"n\":1}\n\n\n{\"name\":\"b\",\"n\":2}\n"
            val decoded: List<Probe> = codec.decodeStream<Probe>(stream).toList()
            assertEquals(listOf(Probe("a", 1), Probe("b", 2)), decoded)
        }

        @Test
        fun `returns empty sequence for empty input`() {
            val decoded: List<Probe> = codec.decodeStream<Probe>("").toList()
            assertTrue(decoded.isEmpty())
        }

        @Test
        fun `returns empty sequence for whitespace-only input`() {
            val decoded: List<Probe> = codec.decodeStream<Probe>("   \n\n  \n").toList()
            assertTrue(decoded.isEmpty())
        }
    }

    @Nested
    inner class EncodeLine {

        @Test
        fun `encodes a value to a single JSON string without trailing newline`() {
            val line: String = codec.encodeLine(Probe("x", 1))
            assertEquals("""{"name":"x","n":1}""", line)
        }

        @Test
        fun `roundtrips decode then encode to an equivalent representation`() {
            val original: Probe = Probe("round", 42)
            val encoded: String = codec.encodeLine(original)
            val decoded: Probe = codec.decodeLine(encoded)
            assertEquals(original, decoded)
        }
    }
}
