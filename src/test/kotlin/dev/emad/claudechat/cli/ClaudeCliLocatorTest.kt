package dev.emad.claudechat.cli

import org.junit.jupiter.api.Nested
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ClaudeCliLocatorTest {

    @Nested
    inner class Probe {

        @Test
        fun `returns info when version output contains a semver`() {
            val executable: Path = Path.of("/usr/bin/claude")
            val locator: ClaudeCliLocator = ClaudeCliLocator(
                versionRunner = { _: Path -> "1.2.3 (Claude Code)" },
            )
            val info: ClaudeCliInfo? = locator.probe(executable)
            assertNotNull(info)
            assertEquals(SemVer(1, 2, 3), info.version)
            assertEquals(executable, info.executable)
        }

        @Test
        fun `returns null when version runner returns null`() {
            val locator: ClaudeCliLocator = ClaudeCliLocator(
                versionRunner = { _: Path -> null },
            )
            assertNull(locator.probe(Path.of("/usr/bin/claude")))
        }

        @Test
        fun `returns null when version runner returns unparseable output`() {
            val locator: ClaudeCliLocator = ClaudeCliLocator(
                versionRunner = { _: Path -> "no version number anywhere" },
            )
            assertNull(locator.probe(Path.of("/usr/bin/claude")))
        }
    }

    @Nested
    inner class LocateOnPath {

        @Test
        fun `returns null when PATH is empty`() {
            val locator: ClaudeCliLocator = ClaudeCliLocator(
                pathEnv = "",
                pathSeparator = ":",
                fileExists = { _: Path -> false },
                versionRunner = { _: Path -> null },
            )
            assertNull(locator.locateOnPath())
        }

        @Test
        fun `returns null when no directory contains the binary`() {
            val locator: ClaudeCliLocator = ClaudeCliLocator(
                pathEnv = "/bin:/usr/bin",
                pathSeparator = ":",
                fileExists = { _: Path -> false },
                versionRunner = { _: Path -> null },
            )
            assertNull(locator.locateOnPath())
        }

        @Test
        fun `returns first valid match in PATH order`() {
            val foundAt: Path = Path.of("/opt/bin/claude")
            val locator: ClaudeCliLocator = ClaudeCliLocator(
                pathEnv = "/usr/bin:/opt/bin:/home/user/bin",
                pathSeparator = ":",
                binaryNames = listOf("claude"),
                fileExists = { path: Path -> path == foundAt },
                versionRunner = { path: Path -> if (path == foundAt) "1.0.40" else null },
            )
            val info: ClaudeCliInfo? = locator.locateOnPath()
            assertNotNull(info)
            assertEquals(foundAt, info.executable)
            assertEquals(SemVer(1, 0, 40), info.version)
        }

        @Test
        fun `tries each candidate binary name in order`() {
            val foundAt: Path = Path.of("/usr/bin/claude.cmd")
            val locator: ClaudeCliLocator = ClaudeCliLocator(
                pathEnv = "/usr/bin",
                pathSeparator = ":",
                binaryNames = listOf("claude", "claude.cmd", "claude.exe"),
                fileExists = { path: Path -> path == foundAt },
                versionRunner = { path: Path -> if (path == foundAt) "2.0.0" else null },
            )
            val info: ClaudeCliInfo? = locator.locateOnPath()
            assertNotNull(info)
            assertEquals(foundAt, info.executable)
        }

        @Test
        fun `ignores blank segments in PATH`() {
            val foundAt: Path = Path.of("/usr/bin/claude")
            val locator: ClaudeCliLocator = ClaudeCliLocator(
                pathEnv = "::/usr/bin::",
                pathSeparator = ":",
                binaryNames = listOf("claude"),
                fileExists = { path: Path -> path == foundAt },
                versionRunner = { _: Path -> "1.0.0" },
            )
            val info: ClaudeCliInfo? = locator.locateOnPath()
            assertNotNull(info)
            assertEquals(foundAt, info.executable)
        }
    }

    @Nested
    inner class MinimumSupportedVersion {

        @Test
        fun `exposes a minimum supported version constant`() {
            assertNotNull(ClaudeCliLocator.MIN_SUPPORTED_VERSION)
        }
    }
}
