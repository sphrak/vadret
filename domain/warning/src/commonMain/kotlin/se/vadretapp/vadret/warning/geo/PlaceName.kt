package se.vadretapp.vadret.warning.geo

private val regionSuffixes: List<String> = listOf(" län", " kommun")

private const val GENITIVE_SUFFIX = "s"

internal fun String.isSamePlaceAs(other: String): Boolean {
    val placeKey: String = toPlaceKey()
    val otherPlaceKey: String = other.toPlaceKey()
    return placeKey == otherPlaceKey ||
        placeKey == otherPlaceKey + GENITIVE_SUFFIX ||
        otherPlaceKey == placeKey + GENITIVE_SUFFIX
}

private fun String.toPlaceKey(): String =
    regionSuffixes
        .fold(initial = trim().lowercase()) { name: String, suffix: String -> name.removeSuffix(suffix = suffix) }
        .trim()