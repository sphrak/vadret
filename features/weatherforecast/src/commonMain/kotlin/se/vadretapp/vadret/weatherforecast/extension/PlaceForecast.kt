package se.vadretapp.vadret.weatherforecast.extension

import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.format.DateTextFormatter
import se.vadretapp.vadret.ui.format.DateTextStyle
import se.vadretapp.vadret.ui.format.toOneDecimalText
import se.vadretapp.vadret.ui.format.toTimeText
import se.vadretapp.vadret.weatherforecast.model.Analysis
import se.vadretapp.vadret.weatherforecast.model.ComingDay
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

internal fun PlaceForecast.toWeatherForecast(
    now: Instant,
    timeZone: TimeZone,
    languageTag: String,
    hourFormat: HourFormat
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
    val comingDays: List<ComingDay> = hours.toComingDays(
        now = now,
        nowHourAt = nowHourAt,
        timeZone = timeZone,
        languageTag = languageTag,
        hourFormat = hourFormat,
        latitude = latitude,
        longitude = longitude
    )
    return WeatherForecast(
        current = current.toCurrentWeather(isDaytime = isDaytime(at = current.validAt)),
        comingWeek = comingDays.toComingWeek(),
        comingDays = comingDays.toImmutableList(),
        days = toForecastDays(
            timeZone = timeZone,
            hourFormat = hourFormat,
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
    hourFormat: HourFormat,
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
                            hourFormat = hourFormat,
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
    hourFormat: HourFormat,
    isDaytime: Boolean
): ForecastHour =
    ForecastHour(
        key = "$HOUR_KEY_PREFIX$validAt",
        timeText = validAt.toTimeText(
            hourFormat = hourFormat,
            timeZone = timeZone
        ),
        temperature = airTemperature,
        feelsLikeTemperature = feelsLikeTemperature(),
        descriptionResource = WeatherForecastUtil.getWsymb2Resource(symbolCode = symbolCode),
        icon = WeatherForecastUtil.getWeatherSymbolIcon(
            symbolCode = symbolCode,
            isDaytime = isDaytime
        )
    )

internal fun PlaceForecast.isAt(followedPlace: FollowedPlace): Boolean =
    placeName == followedPlace.name &&
        latitude == followedPlace.latitude &&
        longitude == followedPlace.longitude