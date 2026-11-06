package se.vadretapp.vadret.weatherforecast.extension

import se.vadretapp.vadret.weatherforecast.local.ForecastEntity
import se.vadretapp.vadret.weatherforecast.model.Forecast
import kotlin.time.Instant

internal fun ForecastEntity.toForecast(): Forecast =
    Forecast(
        validAt = Instant.fromEpochMilliseconds(epochMilliseconds = validAtEpochMillis),
        intervalStartedAt = Instant.fromEpochMilliseconds(epochMilliseconds = intervalStartedAtEpochMillis),
        airTemperature = airTemperature,
        windFromDirection = windFromDirection,
        windSpeed = windSpeed,
        relativeHumidity = relativeHumidity,
        precipitationType = precipitationType,
        symbolCode = symbolCode,
        precipitationAmount = precipitationAmount
    )