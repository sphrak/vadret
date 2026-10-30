package se.vadretapp.vadret.pollen.extension

import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pollen.remote.PollenTypeDto

private const val HAS_FORECASTS_BY_DEFAULT = true

internal fun PollenTypeDto.toPollenType(): PollenType =
    PollenType(
        pollenId = id,
        name = name,
        hasForecasts = hasForecasts ?: HAS_FORECASTS_BY_DEFAULT
    )