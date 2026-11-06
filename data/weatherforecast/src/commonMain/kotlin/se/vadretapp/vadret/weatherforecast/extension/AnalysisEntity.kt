package se.vadretapp.vadret.weatherforecast.extension

import se.vadretapp.vadret.weatherforecast.local.AnalysisEntity
import se.vadretapp.vadret.weatherforecast.model.Analysis
import kotlin.time.Instant

internal fun AnalysisEntity.toAnalysis(): Analysis =
    Analysis(
        analysedAt = Instant.fromEpochMilliseconds(epochMilliseconds = analysedAtEpochMillis),
        airTemperature = airTemperature,
        windFromDirection = windFromDirection,
        windSpeed = windSpeed,
        relativeHumidity = relativeHumidity,
        precipitationType = precipitationType,
        symbolCode = symbolCode,
        precipitationAmountLastHour = precipitationAmountLastHour
    )