package fi.kroon.vadret.ui.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue
import kotlin.time.Instant

private const val TODAY = 0
private const val DAYS_IN_A_WEEK = 7

private val timeFormat = LocalTime.Format {
    hour()
    char(value = ':')
    minute()
}

fun Instant.toDayTimeText(
    now: Instant,
    timeZone: TimeZone,
    languageTag: String
): String {
    val localDateTime: LocalDateTime = toLocalDateTime(timeZone = timeZone)
    val today: LocalDate = now.toLocalDateTime(timeZone = timeZone).date
    val timeText: String = localDateTime.time.format(format = timeFormat)
    val daysAway: Int = today.daysUntil(other = localDateTime.date)
    return when {
        daysAway == TODAY -> timeText
        daysAway.absoluteValue < DAYS_IN_A_WEEK -> {
            val weekdayText: String = dateText(
                dateTextStyle = DateTextStyle.SHORT_WEEKDAY,
                timeZone = timeZone,
                languageTag = languageTag
            )
            "$weekdayText $timeText"
        }
        else -> dateText(
            dateTextStyle = DateTextStyle.DAY_AND_SHORT_MONTH,
            timeZone = timeZone,
            languageTag = languageTag
        )
    }
}

private fun Instant.dateText(
    dateTextStyle: DateTextStyle,
    timeZone: TimeZone,
    languageTag: String
): String =
    DateTextFormatter(
        dateTextStyle = dateTextStyle,
        languageTag = languageTag,
        timeZone = timeZone
    ).format(instant = this)