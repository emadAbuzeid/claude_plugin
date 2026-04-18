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

/**
 * Fábrica del Tool Window principal del plugin.
 *
 * IntelliJ la invoca la primera vez que el usuario activa la ventana registrada en
 * `plugin.xml` con `id="Claude Chat"`. En la versión actual solo construye un
 * [PlaceholderPanel]; las capas reales (transcript + input) se agregan en las
 * fases siguientes (BDGRN-6872 y BDGRN-6873).
 *
 * Implementa [DumbAware] para que la ventana esté disponible también durante la
 * indexación del proyecto.
 */
class ChatToolWindowFactory : ToolWindowFactory, DumbAware {

    /**
     * Rellena el tool window con su contenido inicial.
     *
     * Crea un [Content] no cerrable anclado a un [PlaceholderPanel] y lo agrega al
     * `contentManager`. Usa `displayName` vacío para indicar contenido único sin pestañas.
     *
     * @param project proyecto actual del IDE
     * @param toolWindow tool window de destino, al que se le monta el contenido
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel: PlaceholderPanel = PlaceholderPanel()
        val content: Content = toolWindow.contentManager.factory.createContent(panel, "", false)
        content.isCloseable = false
        toolWindow.contentManager.addContent(content)
    }
}

/**
 * Panel provisional que muestra un texto centrado mientras el resto de las capas del plugin
 * están en desarrollo. Se reemplazará por `ChatPanel` en la Fase 5 (BDGRN-6873).
 */
private class PlaceholderPanel : JBPanel<PlaceholderPanel>(BorderLayout()) {
    init {
        border = JBUI.Borders.empty(16)
        add(
            JBLabel(ClaudeChatBundle.message("toolWindow.placeholder"), SwingConstants.CENTER),
            BorderLayout.CENTER,
        )
    }
}
