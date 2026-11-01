package se.vadretapp.vadret.weatherforecast.model

import kotlin.time.Instant

data class Analysis(
    val analysedAt: Instant,
    val airTemperature: Double,
    val windFromDirection: Double,
    val windSpeed: Double,
    val relativeHumidity: Int,
    val precipitationType: Int,
    val symbolCode: Int,
    val precipitationAmountLastHour: Double?
)