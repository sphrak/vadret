package se.vadretapp.vadret.wind.model

import kotlin.time.Instant

data class WindForecast(
    val referenceTime: Instant,
    val grid: WindGrid,
    val frames: List<WindFrame>
)