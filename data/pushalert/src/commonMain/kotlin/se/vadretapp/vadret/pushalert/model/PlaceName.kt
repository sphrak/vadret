package se.vadretapp.vadret.pushalert.model

import kotlin.jvm.JvmInline

@JvmInline
value class PlaceName(val value: String) {
    init {
        require(value = isPlaceName(value = value)) {
            "A place name is 1 to $MAXIMUM_PLACE_NAME_LENGTH characters without control characters, was: $value"
        }
    }

    companion object {
        fun parseOrNull(raw: String): PlaceName? =
            if (isPlaceName(value = raw)) {
                PlaceName(value = raw)
            } else {
                null
            }
    }
}

private fun isPlaceName(value: String): Boolean =
    value.isNotBlank() && value.length <= MAXIMUM_PLACE_NAME_LENGTH && value.none { character: Char -> character.isISOControl() }

private const val MAXIMUM_PLACE_NAME_LENGTH = 64