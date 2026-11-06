package se.vadretapp.vadret.pollen.extension

import se.vadretapp.vadret.pollen.local.PollenTypeEntity
import se.vadretapp.vadret.pollen.model.PollenType

internal fun PollenTypeEntity.toPollenType(): PollenType =
    PollenType(
        pollenId = pollenId,
        name = name,
        hasForecasts = hasForecasts
    )