package se.vadretapp.vadret.weatherforecast.model

import kotlin.time.Instant

internal data class PlaceForecast(
    val placeName: String?,
    val municipality: String?,
    val county: String?,
    val latitude: Double,
    val longitude: Double,
    val forecasts: List<Forecast>,
    val analyses: List<Analysis>,
    val outdatedSince: Instant?
)