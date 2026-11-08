package se.vadretapp.vadret.aggregatedfeed.model

import se.vadretapp.vadret.geohash.model.Geohash

sealed interface WarningReference {

    fun isReferenceTo(warningIdentifier: String): Boolean

    data class SmhiWarningArea(val warningAreaId: Int) : WarningReference {
        override fun isReferenceTo(warningIdentifier: String): Boolean =
            warningIdentifier.startsWith(prefix = SMHI_IDENTIFIER_PREFIX) &&
                warningIdentifier.endsWith(suffix = "$IDENTIFIER_SEPARATOR$warningAreaId")
    }

    data class Slipperiness(
        val geohash: Geohash,
        val slipperinessDay: String
    ) : WarningReference {
        override fun isReferenceTo(warningIdentifier: String): Boolean =
            warningIdentifier.startsWith(prefix = "$SLIPPERINESS_IDENTIFIER_PREFIX${geohash.value}$IDENTIFIER_SEPARATOR$slipperinessDay$IDENTIFIER_SEPARATOR")
    }
}

private const val IDENTIFIER_SEPARATOR = "-"
private const val SMHI_IDENTIFIER_PREFIX = "smhi-"
private const val SLIPPERINESS_IDENTIFIER_PREFIX = "vadret-slipperiness-"