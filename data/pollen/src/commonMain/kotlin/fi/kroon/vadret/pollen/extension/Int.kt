package fi.kroon.vadret.pollen.extension

import fi.kroon.vadret.pollen.model.PollenLevel

private const val NO_POLLEN = 0
private const val LOW_POLLEN = 1
private const val LOW_TO_MODERATE_POLLEN = 2
private const val MODERATE_POLLEN = 3
private const val MODERATE_TO_HIGH_POLLEN = 4
private const val HIGH_POLLEN = 5
private const val HIGH_TO_VERY_HIGH_POLLEN = 6
private const val VERY_HIGH_POLLEN = 7

internal fun Int.toPollenLevel(): PollenLevel? =
    when (this) {
        NO_POLLEN -> PollenLevel.NONE
        LOW_POLLEN -> PollenLevel.LOW
        LOW_TO_MODERATE_POLLEN -> PollenLevel.LOW_TO_MODERATE
        MODERATE_POLLEN -> PollenLevel.MODERATE
        MODERATE_TO_HIGH_POLLEN -> PollenLevel.MODERATE_TO_HIGH
        HIGH_POLLEN -> PollenLevel.HIGH
        HIGH_TO_VERY_HIGH_POLLEN -> PollenLevel.HIGH_TO_VERY_HIGH
        VERY_HIGH_POLLEN -> PollenLevel.VERY_HIGH
        else -> null
    }