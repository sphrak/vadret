package se.vadretapp.vadret.weatherforecast.extension

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import se.vadretapp.vadret.weatherforecast.model.ComingDay
import se.vadretapp.vadret.weatherforecast.model.ComingWeek
import se.vadretapp.vadret.weatherforecast.model.WeekDay

private const val WEEK_DAY_COUNT = 7
private const val MINIMUM_WEEK_DAY_COUNT = 2
private const val LOWEST_LEVEL = 0f
private const val HIGHEST_LEVEL = 1f
private const val NO_SPAN = 0f
private const val THURSDAY_ISO_DAY_NUMBER = 4
private const val FIRST_DAY_OF_YEAR = 1
private const val FIRST_WEEK_NUMBER = 1
private const val DAYS_IN_WEEK = 7

internal fun List<ComingDay>.withDayToggled(
    dayKey: String,
    isExpandOnceEnabled: Boolean
): ImmutableList<ComingDay> =
    map { comingDay: ComingDay ->
        when {
            comingDay.key == dayKey -> comingDay.copy(isExpanded = !comingDay.isExpanded)
            isExpandOnceEnabled -> comingDay.copy(isExpanded = false)
            else -> comingDay
        }
    }.toImmutableList()

internal fun List<ComingDay>.withDayExpanded(
    dayKey: String,
    isExpandOnceEnabled: Boolean
): ImmutableList<ComingDay> =
    map { comingDay: ComingDay ->
        when {
            comingDay.key == dayKey -> comingDay.copy(isExpanded = true)
            isExpandOnceEnabled -> comingDay.copy(isExpanded = false)
            else -> comingDay
        }
    }.toImmutableList()

internal fun List<ComingDay>.withOtherDaysClosed(
    dayKey: String,
    isExpandOnceEnabled: Boolean
): ImmutableList<ComingDay> =
    map { comingDay: ComingDay ->
        if (isExpandOnceEnabled && comingDay.key != dayKey) {
            comingDay.copy(isExpanded = false)
        } else {
            comingDay
        }
    }.toImmutableList()

internal fun List<ComingDay>.withExpansionOf(previousComingDays: List<ComingDay>): ImmutableList<ComingDay> {
    val expandedDayKeys: Set<String> = previousComingDays
        .filter { comingDay: ComingDay -> comingDay.isExpanded }
        .mapTo(destination = HashSet()) { comingDay: ComingDay -> comingDay.key }
    return map { comingDay: ComingDay ->
        comingDay.copy(isExpanded = comingDay.key in expandedDayKeys)
    }.toImmutableList()
}

internal fun List<ComingDay>.toComingWeek(): ComingWeek? {
    val weekDays: List<ComingDay> = take(n = WEEK_DAY_COUNT)
    if (weekDays.size < MINIMUM_WEEK_DAY_COUNT) {
        return null
    }
    val firstDay: ComingDay = weekDays.first()
    val lastDay: ComingDay = weekDays.last()
    val lowestLevel: Float = weekDays.minOf { comingDay: ComingDay -> comingDay.temperatureRangeStart }
    val highestLevel: Float = weekDays.maxOf { comingDay: ComingDay -> comingDay.temperatureRangeEnd }
    return ComingWeek(
        weekNumberText = weekNumberText(
            firstDate = firstDay.date,
            lastDate = lastDay.date
        ),
        dateRangeText = dateRangeText(
            firstDay = firstDay,
            lastDay = lastDay
        ),
        days = weekDays.map { comingDay: ComingDay ->
            comingDay.toWeekDay(
                lowestLevel = lowestLevel,
                highestLevel = highestLevel
            )
        }.toImmutableList()
    )
}

private fun weekNumberText(
    firstDate: LocalDate,
    lastDate: LocalDate
): String {
    val firstWeekNumber: Int = firstDate.isoWeekNumber()
    val lastWeekNumber: Int = lastDate.isoWeekNumber()
    return if (firstWeekNumber == lastWeekNumber) {
        "$firstWeekNumber"
    } else {
        "$firstWeekNumber-$lastWeekNumber"
    }
}

// A week belongs to the year its Thursday falls in, so counting Thursdays gives the week number.
private fun LocalDate.isoWeekNumber(): Int {
    val thursday: LocalDate = plus(
        value = THURSDAY_ISO_DAY_NUMBER - dayOfWeek.isoDayNumber,
        unit = DateTimeUnit.DAY
    )
    return (thursday.dayOfYear - FIRST_DAY_OF_YEAR) / DAYS_IN_WEEK + FIRST_WEEK_NUMBER
}

private fun dateRangeText(
    firstDay: ComingDay,
    lastDay: ComingDay
): String {
    val firstDateText: String = if (firstDay.date.month == lastDay.date.month) {
        "${firstDay.date.day}"
    } else {
        firstDay.dateText
    }
    return "$firstDateText-${lastDay.dateText}"
}

// The day ranges share one linear scale over every coming day, so stretching them over the week keeps each day where its temperatures put it.
private fun ComingDay.toWeekDay(
    lowestLevel: Float,
    highestLevel: Float
): WeekDay =
    WeekDay(
        dayKey = key,
        isToday = isToday,
        weekdayText = weekdayText,
        icon = icon,
        descriptionResource = descriptionResource,
        precipitationText = precipitationText,
        lowTemperatureText = lowTemperatureText,
        highTemperatureText = highTemperatureText,
        lowTemperatureLevel = temperatureRangeStart.withinWeek(
            lowestLevel = lowestLevel,
            highestLevel = highestLevel,
            fallback = LOWEST_LEVEL
        ),
        highTemperatureLevel = temperatureRangeEnd.withinWeek(
            lowestLevel = lowestLevel,
            highestLevel = highestLevel,
            fallback = HIGHEST_LEVEL
        )
    )

private fun Float.withinWeek(
    lowestLevel: Float,
    highestLevel: Float,
    fallback: Float
): Float {
    val span: Float = highestLevel - lowestLevel
    return if (span > NO_SPAN) {
        (this - lowestLevel) / span
    } else {
        fallback
    }
}