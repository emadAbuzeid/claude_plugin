package dev.emad.claudechat.cli

import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SemVerTest {

    @Nested
    inner class ParseOrNull {

        @Test
        fun `parses plain major dot minor dot patch`() {
            assertEquals(SemVer(1, 2, 3), SemVer.parseOrNull("1.2.3"))
        }

        @Test
        fun `parses version with leading v prefix`() {
            assertEquals(SemVer(1, 2, 3), SemVer.parseOrNull("v1.2.3"))
        }

        @Test
        fun `strips pre-release suffix and keeps major minor patch`() {
            assertEquals(SemVer(1, 2, 3), SemVer.parseOrNull("1.2.3-beta.1"))
        }

        @Test
        fun `finds version embedded in surrounding text`() {
            val text: String = "Claude Code version 1.2.3 (build abc)"
            assertEquals(SemVer(1, 2, 3), SemVer.parseOrNull(text))
        }

        @Test
        fun `returns null when no version present`() {
            assertNull(SemVer.parseOrNull("hello world"))
        }

        @Test
        fun `returns null for incomplete version without patch`() {
            assertNull(SemVer.parseOrNull("1.2"))
        }

        @Test
        fun `parses multi-digit components`() {
            assertEquals(SemVer(12, 34, 567), SemVer.parseOrNull("12.34.567"))
        }
    }

    @Nested
    inner class Ordering {

        @Test
        fun `patch bump is greater`() {
            assertTrue(SemVer(1, 0, 0) < SemVer(1, 0, 1))
        }

        @Test
        fun `minor bump outweighs any patch`() {
            assertTrue(SemVer(1, 0, 99) < SemVer(1, 1, 0))
        }

        @Test
        fun `major bump outweighs any minor`() {
            assertTrue(SemVer(1, 99, 99) < SemVer(2, 0, 0))
        }

        @Test
        fun `equal versions compare to zero`() {
            assertEquals(0, SemVer(1, 2, 3).compareTo(SemVer(1, 2, 3)))
        }
    }

    @Nested
    inner class Representation {

        @Test
        fun `toString renders major dot minor dot patch`() {
            assertEquals("1.2.3", SemVer(1, 2, 3).toString())
        }
    }
}
