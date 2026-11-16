package se.vadretapp.vadret.weatherforecast.extension

import se.vadretapp.vadret.weatherforecast.local.AnalysisEntity
import se.vadretapp.vadret.weatherforecast.remote.AnalysisTimeSeriesDto
import kotlin.time.Instant

private const val MISSING_VALUE = 9999.0
private const val LOWEST_VALUE = 0.0

internal fun AnalysisTimeSeriesDto.toAnalysisEntity(): AnalysisEntity =
    AnalysisEntity(
        analysedAtEpochMillis = Instant.parse(input = time).toEpochMilliseconds(),
        airTemperature = data.airTemperature,
        windFromDirection = data.windFromDirection,
        windSpeed = data.windSpeed,
        windGustSpeed = data.windGustSpeed.orNullIfMissing(),
        relativeHumidity = data.relativeHumidity,
        precipitationType = data.precipitationType,
        symbolCode = data.symbolCode,
        precipitationAmountLastHour = data.precipitationAmountLastHour.orNullIfMissing()
    )

private fun Double?.orNullIfMissing(): Double? =
    if (this == null || this >= MISSING_VALUE || this < LOWEST_VALUE) {
        null
    } else {
        this
    }