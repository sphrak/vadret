package se.vadretapp.vadret.weatherforecast.extension

import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.format.DateTextFormatter
import se.vadretapp.vadret.ui.format.DateTextStyle
import se.vadretapp.vadret.ui.format.toHourText
import se.vadretapp.vadret.ui.format.toOneDecimalText
import se.vadretapp.vadret.weatherforecast.model.ComingDay
import se.vadretapp.vadret.weatherforecast.model.ComingHour
import se.vadretapp.vadret.weatherforecast.model.Forecast
import se.vadretapp.vadret.weatherforecast.util.SunsetUtil
import se.vadretapp.vadret.weatherforecast.util.WeatherForecastUtil
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Instant

private const val COMING_DAY_KEY_PREFIX = "coming-day-"
private const val COMING_HOUR_KEY_PREFIX = "coming-hour-"
private const val DAYTIME_START_HOUR = 6
private const val DAYTIME_END_HOUR = 18
private const val MIDDAY_HOUR = 12
private const val MIDDAY_MINUTE = 0
private const val FIRST_HOUR_INDEX = 0
private const val NO_HOUR_INDEX = -1
private const val MIDPOINT_DIVISOR = 2
private const val SHOWN_PRECIPITATION = 0.1
private const val RANGE_START = 0f
private const val RANGE_END = 1f
private const val NO_SPAN = 0.0
private const val MINIMUM_HOUR_TEMPERATURE_SPAN = 4.0
private const val FULL_WIND_LEVEL_SPEED = 12.0
private const val NO_LEVEL = 0.0
private const val FULL_LEVEL = 1.0
private const val WIND_ARROW_ROTATION_OFFSET = 180f
private const val DEGREES_IN_CIRCLE = 360f

private class DaySummary(
    val date: LocalDate,
    val forecasts: List<Forecast>,
    val lowTemperature: Double,
    val highTemperature: Double,
    val precipitationAmount: Double?
)

private class TemperatureScale(
    val low: Double,
    val high: Double
)

private class DayFormats(
    val weekdayFormatter: DateTextFormatter,
    val dateFormatter: DateTextFormatter,
    val hourFormat: HourFormat
)

internal fun List<Forecast>.toComingDays(
    now: Instant,
    nowHourAt: Instant?,
    timeZone: TimeZone,
    languageTag: String,
    hourFormat: HourFormat,
    latitude: Double,
    longitude: Double
): List<ComingDay> {
    val today: LocalDate = now.toLocalDateTime(timeZone = timeZone).date
    val summaries: List<DaySummary> = toDaySummaries(timeZone = timeZone)
        .filter { daySummary: DaySummary ->
            daySummary.date == today || daySummary.hasDaytime(timeZone = timeZone)
        }
    if (summaries.isEmpty()) {
        return emptyList()
    }
    val temperatureScale = TemperatureScale(
        low = summaries.minOf { daySummary: DaySummary -> daySummary.lowTemperature },
        high = summaries.maxOf { daySummary: DaySummary -> daySummary.highTemperature }
    )
    val dayFormats = DayFormats(
        weekdayFormatter = DateTextFormatter(
            dateTextStyle = DateTextStyle.SHORT_WEEKDAY,
            languageTag = languageTag,
            timeZone = timeZone
        ),
        dateFormatter = DateTextFormatter(
            dateTextStyle = DateTextStyle.DAY_AND_SHORT_MONTH,
            languageTag = languageTag,
            timeZone = timeZone
        ),
        hourFormat = hourFormat
    )
    return summaries.map { daySummary: DaySummary ->
        daySummary.toComingDay(
            isToday = daySummary.date == today,
            now = now,
            nowHourAt = nowHourAt,
            timeZone = timeZone,
            latitude = latitude,
            longitude = longitude,
            temperatureScale = temperatureScale,
            dayFormats = dayFormats
        )
    }
}

private fun List<Forecast>.toDaySummaries(timeZone: TimeZone): List<DaySummary> {
    val precipitationByDate: Map<LocalDate, Double?> = groupBy { forecast: Forecast ->
        forecast.intervalMidpoint().toLocalDateTime(timeZone = timeZone).date
    }.mapValues { (_, intervalForecasts: List<Forecast>) ->
        intervalForecasts.totalPrecipitationAmount()
    }
    return groupBy { forecast: Forecast -> forecast.validAt.toLocalDateTime(timeZone = timeZone).date }
        .map { (date: LocalDate, dayForecasts: List<Forecast>) ->
            DaySummary(
                date = date,
                forecasts = dayForecasts,
                lowTemperature = dayForecasts.minOf { forecast: Forecast -> forecast.airTemperature },
                highTemperature = dayForecasts.maxOf { forecast: Forecast -> forecast.airTemperature },
                precipitationAmount = precipitationByDate[date]
            )
        }
}

private fun Forecast.intervalMidpoint(): Instant {
    val intervalLength: Duration = validAt - intervalStartedAt
    return intervalStartedAt + intervalLength / MIDPOINT_DIVISOR
}

private fun List<Forecast>.totalPrecipitationAmount(): Double? {
    val knownAmounts: List<Double> = mapNotNull { forecast: Forecast -> forecast.precipitationAmount }
    return if (knownAmounts.size == size) {
        knownAmounts.sum()
    } else {
        null
    }
}

private fun DaySummary.hasDaytime(timeZone: TimeZone): Boolean =
    forecasts.any { forecast: Forecast ->
        forecast.isDaytimeHour(timeZone = timeZone)
    }

private fun Forecast.isDaytimeHour(timeZone: TimeZone): Boolean =
    validAt
        .toLocalDateTime(timeZone = timeZone)
        .hour in DAYTIME_START_HOUR..DAYTIME_END_HOUR

private fun DaySummary.toComingDay(
    isToday: Boolean,
    now: Instant,
    nowHourAt: Instant?,
    timeZone: TimeZone,
    latitude: Double,
    longitude: Double,
    temperatureScale: TemperatureScale,
    dayFormats: DayFormats
): ComingDay {
    val midday: Instant = date
        .atTime(
            hour = MIDDAY_HOUR,
            minute = MIDDAY_MINUTE
        ).toInstant(timeZone = timeZone)
    val symbolCode: Int = representativeSymbolCode(timeZone = timeZone)
    val hourTemperatureScale: TemperatureScale = hourTemperatureScale()
    val nowHourIndex: Int = forecasts.indexOfFirst { forecast: Forecast -> forecast.validAt == nowHourAt }
    val shownFrom: Instant? = if (isToday) {
        now
    } else {
        SunsetUtil.getSunriseSunset(
            day = midday,
            latitude = latitude,
            longitude = longitude
        )?.sunrise
    }
    return ComingDay(
        key = "$COMING_DAY_KEY_PREFIX$date",
        date = date,
        isToday = isToday,
        weekdayText = dayFormats.weekdayFormatter
            .format(instant = midday)
            .replaceFirstChar { character: Char -> character.titlecase() },
        dateText = dayFormats.dateFormatter.format(instant = midday),
        icon = WeatherForecastUtil.getWeatherSymbolIcon(
            symbolCode = symbolCode,
            isDaytime = true
        ),
        descriptionResource = WeatherForecastUtil.getWsymb2Resource(symbolCode = symbolCode),
        precipitationText = precipitationAmount.toPrecipitationText(),
        lowTemperatureText = lowTemperature.roundToInt().toString(),
        highTemperatureText = highTemperature.roundToInt().toString(),
        lowTemperature = lowTemperature,
        highTemperature = highTemperature,
        temperatureRangeStart = temperatureScale.fractionOf(
            temperature = lowTemperature,
            fallback = RANGE_START
        ),
        temperatureRangeEnd = temperatureScale.fractionOf(
            temperature = highTemperature,
            fallback = RANGE_END
        ),
        hours = forecasts.mapIndexed { index: Int, forecast: Forecast ->
            forecast.toComingHour(
                isNow = index == nowHourIndex,
                timeZone = timeZone,
                dayFormats = dayFormats,
                isDaytime = SunsetUtil.isSunUp(
                    at = forecast.validAt,
                    latitude = latitude,
                    longitude = longitude
                ),
                temperatureScale = hourTemperatureScale
            )
        }.toImmutableList(),
        firstShownHourIndex = firstShownHourIndex(
            nowHourIndex = nowHourIndex,
            shownFrom = shownFrom
        ),
        isExpanded = false
    )
}

private fun DaySummary.representativeSymbolCode(timeZone: TimeZone): Int {
    val daytimeForecasts: List<Forecast> = forecasts.filter { forecast: Forecast ->
        forecast.isDaytimeHour(timeZone = timeZone)
    }
    return daytimeForecasts
        .ifEmpty { forecasts }
        .groupingBy { forecast: Forecast -> forecast.symbolCode }
        .eachCount()
        .entries
        .maxWith(
            comparator = compareBy<Map.Entry<Int, Int>> { entry: Map.Entry<Int, Int> -> entry.value }
                .thenBy { entry: Map.Entry<Int, Int> -> entry.key }
        ).key
}

private fun TemperatureScale.fractionOf(
    temperature: Double,
    fallback: Float
): Float {
    val span: Double = high - low
    return if (span > NO_SPAN) {
        ((temperature - low) / span).toFloat()
    } else {
        fallback
    }
}

private fun DaySummary.hourTemperatureScale(): TemperatureScale {
    if (highTemperature - lowTemperature >= MINIMUM_HOUR_TEMPERATURE_SPAN) {
        return TemperatureScale(
            low = lowTemperature,
            high = highTemperature
        )
    }
    val middle: Double = (lowTemperature + highTemperature) / MIDPOINT_DIVISOR
    return TemperatureScale(
        low = middle - MINIMUM_HOUR_TEMPERATURE_SPAN / MIDPOINT_DIVISOR,
        high = middle + MINIMUM_HOUR_TEMPERATURE_SPAN / MIDPOINT_DIVISOR
    )
}

private fun DaySummary.firstShownHourIndex(
    nowHourIndex: Int,
    shownFrom: Instant?
): Int {
    if (nowHourIndex != NO_HOUR_INDEX) {
        return nowHourIndex
    }
    if (shownFrom == null) {
        return FIRST_HOUR_INDEX
    }
    return forecasts
        .indexOfLast { forecast: Forecast -> forecast.validAt <= shownFrom }
        .coerceAtLeast(minimumValue = FIRST_HOUR_INDEX)
}

private fun Forecast.toComingHour(
    isNow: Boolean,
    timeZone: TimeZone,
    dayFormats: DayFormats,
    isDaytime: Boolean,
    temperatureScale: TemperatureScale
): ComingHour =
    ComingHour(
        key = "$COMING_HOUR_KEY_PREFIX$validAt",
        isNow = isNow,
        timeText = validAt.toHourText(
            hourFormat = dayFormats.hourFormat,
            timeZone = timeZone
        ),
        icon = WeatherForecastUtil.getWeatherSymbolIcon(
            symbolCode = symbolCode,
            isDaytime = isDaytime
        ),
        descriptionResource = WeatherForecastUtil.getWsymb2Resource(symbolCode = symbolCode),
        temperatureText = airTemperature.roundToInt().toString(),
        temperatureLevel = temperatureScale.fractionOf(
            temperature = airTemperature,
            fallback = RANGE_START
        ),
        precipitationText = precipitationAmount.toPrecipitationText(),
        windSpeedText = windSpeed.toOneDecimalText(),
        windLevel = (windSpeed / FULL_WIND_LEVEL_SPEED)
            .coerceIn(
                minimumValue = NO_LEVEL,
                maximumValue = FULL_LEVEL
            ).toFloat(),
        windArrowRotation = windArrowRotation(),
        windDirectionResource = WeatherForecastUtil.handleWindDirection(degree = windFromDirection)
    )

internal fun Forecast.windArrowRotation(): Float = (windFromDirection.toFloat() + WIND_ARROW_ROTATION_OFFSET) % DEGREES_IN_CIRCLE

private fun Double?.toPrecipitationText(): String? =
    if (this != null && this >= SHOWN_PRECIPITATION) {
        toOneDecimalText()
    } else {
        null
    }