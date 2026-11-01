package se.vadretapp.vadret.pollen.extension

import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pollen.remote.PollenTypeDto

internal fun PollenTypeDto.toPollenType(): PollenType =
    PollenType(
        pollenId = id,
        name = name
    )