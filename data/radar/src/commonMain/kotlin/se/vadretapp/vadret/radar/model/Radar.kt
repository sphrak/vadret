package se.vadretapp.vadret.radar.model

import kotlin.time.Instant

data class Radar(
    val images: List<RadarImage>,
    val fetchedAt: Instant
)