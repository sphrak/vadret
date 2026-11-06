package se.vadretapp.vadret.radar.model

import kotlin.time.Instant

data class SavedRadar(
    val radar: Radar,
    val fetchedAt: Instant?
)