package dev.emad.claudechat

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * Iconos vectoriales del plugin, cargados perezosamente desde `resources/icons/`.
 *
 * Expuestos como `@JvmField` para que se puedan referenciar directamente desde `plugin.xml`
 * con la notación `dev.emad.claudechat.ClaudeChatIcons.ToolWindow`.
 */
object ClaudeChatIcons {
    /**
     * Icono del Tool Window (13×13, monocromático adaptativo al tema claro/oscuro).
     *
     * Referenciado desde el atributo `icon` de la extensión `com.intellij.toolWindow`.
     */
    @JvmField
    val ToolWindow: Icon = IconLoader.getIcon("/icons/claudeToolWindow.svg", ClaudeChatIcons::class.java)
}
