package dev.emad.claudechat.ui

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.Content
import com.intellij.util.ui.JBUI
import dev.emad.claudechat.ClaudeChatBundle
import java.awt.BorderLayout
import javax.swing.SwingConstants

class ChatToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel: PlaceholderPanel = PlaceholderPanel()
        val content: Content = toolWindow.contentManager.factory.createContent(panel, "", false)
        content.isCloseable = false
        toolWindow.contentManager.addContent(content)
    }
}

private class PlaceholderPanel : JBPanel<PlaceholderPanel>(BorderLayout()) {
    init {
        border = JBUI.Borders.empty(16)
        add(
            JBLabel(ClaudeChatBundle.message("toolWindow.placeholder"), SwingConstants.CENTER),
            BorderLayout.CENTER,
        )
    }
}
