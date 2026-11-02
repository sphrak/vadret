package se.vadretapp.vadret.weatherforecast.extension

import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import se.vadretapp.vadret.location.model.StoredLocation
import se.vadretapp.vadret.ui.format.DateTextFormatter
import se.vadretapp.vadret.ui.format.DateTextStyle
import se.vadretapp.vadret.ui.format.toOneDecimalText
import se.vadretapp.vadret.weatherforecast.model.Analysis
import se.vadretapp.vadret.weatherforecast.model.CurrentWeather
import se.vadretapp.vadret.weatherforecast.model.Forecast
import se.vadretapp.vadret.weatherforecast.model.ForecastDay
import se.vadretapp.vadret.weatherforecast.model.ForecastHour
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast
import se.vadretapp.vadret.weatherforecast.model.WeatherForecast
import se.vadretapp.vadret.weatherforecast.util.SunsetUtil
import se.vadretapp.vadret.weatherforecast.util.WeatherForecastUtil
import se.vadretapp.vadret.weatherforecast.util.WindChill
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private const val DAY_KEY_PREFIX = "day-"
private const val HOUR_KEY_PREFIX = "hour-"

private val nowHourTolerance: Duration = 1.hours

private val hourFormat = LocalTime.Format {
    hour()
    char(value = ':')
    minute()
}

internal fun PlaceForecast.toWeatherForecast(
    now: Instant,
    timeZone: TimeZone,
    languageTag: String
): WeatherForecast {
    val first: Forecast = forecasts.firstOrNull() ?: return WeatherForecast()
    val hours: List<Forecast> = analysesOfToday(
        firstForecast = first,
        now = now,
        timeZone = timeZone
    ) + forecasts
    val current: Forecast = hours.minBy { forecast: Forecast -> (forecast.validAt - now).absoluteValue }
    val nowHourAt: Instant? = if ((current.validAt - now).absoluteValue <= nowHourTolerance) {
        current.validAt
    } else {
        null
    }
    return WeatherForecast(
        current = current.toCurrentWeather(isDaytime = isDaytime(at = current.validAt)),
        comingDays = hours.toComingDays(
            now = now,
            nowHourAt = nowHourAt,
            timeZone = timeZone,
            languageTag = languageTag,
            latitude = latitude,
            longitude = longitude
        ).toImmutableList(),
        days = toForecastDays(
            timeZone = timeZone,
            dateFormatter = DateTextFormatter(
                dateTextStyle = DateTextStyle.LONG_DATE,
                languageTag = languageTag,
                timeZone = timeZone
            ),
            weekdayFormatter = DateTextFormatter(
                dateTextStyle = DateTextStyle.WEEKDAY,
                languageTag = languageTag,
                timeZone = timeZone
            )
        ).toImmutableList()
    )
}

private fun PlaceForecast.analysesOfToday(
    firstForecast: Forecast,
    now: Instant,
    timeZone: TimeZone
): List<Forecast> {
    val startOfToday: Instant = now
        .toLocalDateTime(timeZone = timeZone)
        .date
        .atStartOfDayIn(timeZone = timeZone)
    return analyses
        .filter { analysis: Analysis ->
            analysis.analysedAt >= startOfToday && analysis.analysedAt < firstForecast.validAt
        }.map { analysis: Analysis -> analysis.toForecast() }
}

// Forecasts come first, so a forecast wins over an analysis of the same hour as it does in the full forecast.
internal fun PlaceForecast.toCurrentWeather(now: Instant): CurrentWeather? {
    val hours: List<Forecast> = forecasts + analyses.map { analysis: Analysis -> analysis.toForecast() }
    val current: Forecast = hours.minByOrNull { forecast: Forecast -> (forecast.validAt - now).absoluteValue } ?: return null
    return current.toCurrentWeather(isDaytime = isDaytime(at = current.validAt))
}

private fun PlaceForecast.isDaytime(at: Instant): Boolean =
    SunsetUtil.isSunUp(
        at = at,
        latitude = latitude,
        longitude = longitude
    )

private fun PlaceForecast.toForecastDays(
    timeZone: TimeZone,
    dateFormatter: DateTextFormatter,
    weekdayFormatter: DateTextFormatter
): List<ForecastDay> =
    forecasts
        .groupBy { forecast: Forecast -> forecast.validAt.toLocalDateTime(timeZone = timeZone).date }
        .map { (date: LocalDate, dayForecasts: List<Forecast>) ->
            val startOfDay: Instant = date.atStartOfDayIn(timeZone = timeZone)
            ForecastDay(
                key = "$DAY_KEY_PREFIX$date",
                weekday = weekdayFormatter
                    .format(instant = startOfDay)
                    .replaceFirstChar { character: Char -> character.titlecase() },
                dateText = dateFormatter.format(instant = startOfDay),
                hours = dayForecasts
                    .map { forecast: Forecast ->
                        forecast.toForecastHour(
                            timeZone = timeZone,
                            isDaytime = isDaytime(at = forecast.validAt)
                        )
                    }.toImmutableList()
            )
        }

private fun Forecast.toCurrentWeather(isDaytime: Boolean): CurrentWeather =
    CurrentWeather(
        temperatureText = airTemperature.toTemperatureText(),
        feelsLikeTemperatureText = (windChill() ?: airTemperature).toTemperatureText(),
        icon = WeatherForecastUtil.getWeatherSymbolIcon(
            symbolCode = symbolCode,
            isDaytime = isDaytime
        ),
        descriptionResource = WeatherForecastUtil.getWsymb2Resource(symbolCode = symbolCode),
        windDirectionResource = WeatherForecastUtil.handleWindDirection(degree = windFromDirection),
        windSpeed = windSpeed,
        windArrowRotation = windArrowRotation(),
        humidityPercent = relativeHumidity
    )

private fun Forecast.windChill(): Double? =
    WindChill.calculate(
        temperatureCelcius = airTemperature,
        windSpeedMeterPerSecond = windSpeed
    )

private fun Forecast.feelsLikeTemperature(): String? {
    val windChill: Double = windChill() ?: return null
    return windChill.toOneDecimalText()
}

private fun Forecast.toForecastHour(
    timeZone: TimeZone,
    isDaytime: Boolean
): ForecastHour =
    ForecastHour(
        key = "$HOUR_KEY_PREFIX$validAt",
        timeText = validAt
            .toLocalDateTime(timeZone = timeZone)
            .time
            .format(format = hourFormat),
        temperature = airTemperature,
        feelsLikeTemperature = feelsLikeTemperature(),
        descriptionResource = WeatherForecastUtil.getWsymb2Resource(symbolCode = symbolCode),
        icon = WeatherForecastUtil.getWeatherSymbolIcon(
            symbolCode = symbolCode,
            isDaytime = isDaytime
        )
    )

internal fun PlaceForecast.isAt(storedLocation: StoredLocation): Boolean =
    placeName == storedLocation.locality &&
        latitude == storedLocation.latitude &&
        longitude == storedLocation.longitude