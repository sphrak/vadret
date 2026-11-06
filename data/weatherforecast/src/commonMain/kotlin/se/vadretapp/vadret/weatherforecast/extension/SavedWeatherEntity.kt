package se.vadretapp.vadret.weatherforecast.extension

import se.vadretapp.vadret.weatherforecast.local.AnalysisEntity
import se.vadretapp.vadret.weatherforecast.local.ForecastEntity
import se.vadretapp.vadret.weatherforecast.local.SavedWeatherEntity
import se.vadretapp.vadret.weatherforecast.model.PlaceWeather
import kotlin.time.Instant

internal fun SavedWeatherEntity.toPlaceWeather(): PlaceWeather? {
    val fetchedAt: Long = fetchedAtEpochMillis ?: return null
    return PlaceWeather(
        forecasts = forecasts.map { forecastEntity: ForecastEntity -> forecastEntity.toForecast() },
        analyses = analyses.map { analysisEntity: AnalysisEntity -> analysisEntity.toAnalysis() },
        fetchedAt = Instant.fromEpochMilliseconds(epochMilliseconds = fetchedAt)
    )
}