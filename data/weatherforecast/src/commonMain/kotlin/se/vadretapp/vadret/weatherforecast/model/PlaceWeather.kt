package se.vadretapp.vadret.weatherforecast.model

import kotlin.time.Instant

data class PlaceWeather(
    val forecasts: List<Forecast>,
    val analyses: List<Analysis>,
    val fetchedAt: Instant?
)