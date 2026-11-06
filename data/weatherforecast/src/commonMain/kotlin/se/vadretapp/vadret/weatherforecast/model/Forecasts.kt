package se.vadretapp.vadret.weatherforecast.model

import kotlin.time.Instant

data class Forecasts(
    val forecasts: List<Forecast>,
    val fetchedAt: Instant
)