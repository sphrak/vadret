package se.vadretapp.vadret.weatherforecast.extension

import se.vadretapp.vadret.weatherforecast.local.AnalysisEntity
import se.vadretapp.vadret.weatherforecast.local.ForecastEntity
import se.vadretapp.vadret.weatherforecast.local.SavedWeatherEntity
import se.vadretapp.vadret.weatherforecast.model.PlaceWeather
import kotlin.time.Instant

internal fun SavedWeatherEntity?.toPlaceWeather(): PlaceWeather =
    PlaceWeather(
        forecasts = this?.forecasts.orEmpty().map { forecastEntity: ForecastEntity -> forecastEntity.toForecast() },
        analyses = this?.analyses.orEmpty().map { analysisEntity: AnalysisEntity -> analysisEntity.toAnalysis() },
        fetchedAt = if (this != null) {
            Instant.fromEpochMilliseconds(epochMilliseconds = fetchedAtEpochMillis)
        } else {
            null
        }
    )