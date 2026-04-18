package dev.emad.claudechat

import java.util.Locale
import java.util.ResourceBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClaudeChatBundleTest {

    private val bundleName: String = "messages.ClaudeChatBundle"

    @Test
    fun `resource bundle contains every key declared in ClaudeChatBundle properties`() {
        val bundle: ResourceBundle = ResourceBundle.getBundle(bundleName, Locale.ROOT)
        val expectedKeys: Set<String> = setOf(
            "toolWindow.title",
            "toolWindow.placeholder",
            "session.new",
            "input.placeholder",
            "button.send",
            "button.stop",
            "error.cliNotFound",
        )
        for (key: String in expectedKeys) {
            assertTrue(bundle.containsKey(key), "Missing bundle key: $key")
        }
    }

    @Test
    fun `toolWindow title resolves to Claude Chat`() {
        val bundle: ResourceBundle = ResourceBundle.getBundle(bundleName, Locale.ROOT)
        val resolved: String = bundle.getString("toolWindow.title")
        assertEquals("Claude Chat", resolved)
    }

    @Test
    fun `ClaudeChatBundle message resolves key without application context`() {
        val resolved: String = ClaudeChatBundle.message("toolWindow.title")
        assertTrue(resolved.isNotBlank(), "Expected non-blank message for toolWindow.title")
        assertEquals("Claude Chat", resolved)
    }
}
