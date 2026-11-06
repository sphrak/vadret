package se.vadretapp.vadret.pollen.model

import kotlin.time.Instant

data class PollenTypes(
    val pollenTypes: List<PollenType>,
    val fetchedAt: Instant
)