package fi.kroon.vadret.pollen.extension

import fi.kroon.vadret.pollen.model.PollenType
import fi.kroon.vadret.pollen.remote.PollenTypeDto

internal fun PollenTypeDto.toPollenType(): PollenType =
    PollenType(
        pollenId = id,
        name = name
    )