package dev.emad.claudechat.cli

/**
 * Representación reducida de una versión semántica — solo `major.minor.patch`.
 *
 * Descarta pre-releases y metadata al parsear (ej. `1.2.3-beta.1` se vuelve `1.2.3`).
 * Usado principalmente para comparar la versión del CLI de Claude contra el mínimo
 * soportado por el plugin.
 *
 * @property major número mayor (incrementa en cambios incompatibles)
 * @property minor número menor (incrementa en funcionalidades compatibles)
 * @property patch número de parche (incrementa en fixes compatibles)
 */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemVer> {

    /**
     * Comparación lexicográfica sobre `(major, minor, patch)`.
     *
     * @param other versión a comparar
     * @return negativo si `this` es anterior, cero si igual, positivo si posterior
     */
    override fun compareTo(other: SemVer): Int = compareValuesBy(
        this,
        other,
        SemVer::major,
        SemVer::minor,
        SemVer::patch,
    )

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val PATTERN: Regex = Regex("""(\d+)\.(\d+)\.(\d+)""")

        /**
         * Intenta extraer una versión `X.Y.Z` del texto dado.
         *
         * Tolera prefijos como `v`, y sufijos como `-beta`, `-rc.1` o metadata `+build.1` —
         * busca el primer trío de enteros separados por puntos dentro de la cadena.
         *
         * @param text texto arbitrario donde buscar la versión (ej. salida de `claude --version`)
         * @return la [SemVer] encontrada, o `null` si no se encontró ningún trío válido
         */
        fun parseOrNull(text: String): SemVer? {
            val match: MatchResult = PATTERN.find(text) ?: return null
            val major: String = match.groupValues[1]
            val minor: String = match.groupValues[2]
            val patch: String = match.groupValues[3]
            return SemVer(major.toInt(), minor.toInt(), patch.toInt())
        }
    }
}
