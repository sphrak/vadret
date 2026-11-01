package se.vadretapp.vadret.weatherforecast.extension

import se.vadretapp.vadret.weatherforecast.model.Forecast
import se.vadretapp.vadret.weatherforecast.remote.TimeSeriesDto
import kotlin.time.Instant

private const val MISSING_VALUE = 9999.0
private const val NO_PRECIPITATION = 0.0

internal fun TimeSeriesDto.toForecast(): Forecast =
    Forecast(
        validAt = Instant.parse(input = time),
        intervalStartedAt = Instant.parse(input = intervalParametersStartTime),
        airTemperature = data.airTemperature,
        windFromDirection = data.windFromDirection,
        windSpeed = data.windSpeed,
        relativeHumidity = data.relativeHumidity,
        precipitationType = data.precipitationType,
        symbolCode = data.symbolCode,
        precipitationAmount = data.precipitationAmountMean.toPrecipitationAmount()
    )

private fun Double?.toPrecipitationAmount(): Double? =
    if (this == null || this >= MISSING_VALUE || this < NO_PRECIPITATION) {
        null
    } else {
        this
    }