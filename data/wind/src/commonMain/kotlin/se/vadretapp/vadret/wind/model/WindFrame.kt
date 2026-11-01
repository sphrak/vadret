package se.vadretapp.vadret.wind.model

import kotlin.time.Instant

data class WindFrame(
    val validAt: Instant,
    val windSpeeds: List<Float>,
    val windFromDirections: List<Float>
)