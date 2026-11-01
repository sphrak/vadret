package se.vadretapp.vadret.radar.model

import kotlin.time.Instant

data class RadarImage(
    val imageUrl: String,
    val validAt: Instant
)