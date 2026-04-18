package dev.emad.claudechat

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey

private const val BUNDLE: String = "messages.ClaudeChatBundle"

/**
 * Bundle central de internacionalización del plugin.
 *
 * Carga las cadenas definidas en `src/main/resources/messages/ClaudeChatBundle.properties`
 * respetando el idioma activo del IDE. Hereda de [DynamicBundle] para integrarse con el
 * mecanismo estándar de recursos de IntelliJ.
 */
object ClaudeChatBundle : DynamicBundle(BUNDLE) {

    /**
     * Devuelve la cadena traducida asociada a la clave dada.
     *
     * Si `params` no está vacío, la cadena resultante se formatea reemplazando los
     * placeholders `{0}`, `{1}`, ... por los argumentos correspondientes (MessageFormat).
     *
     * @param key clave de la propiedad; debe existir en `ClaudeChatBundle.properties`
     * @param params argumentos opcionales para sustituir placeholders
     * @return cadena localizada y formateada
     */
    @Nls
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)
}
