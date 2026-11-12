package se.vadretapp.vadret.ui.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import se.vadretapp.vadret.setting.model.HourFormat
import kotlin.math.absoluteValue
import kotlin.time.Instant

private const val TODAY = 0
private const val DAYS_IN_A_WEEK = 7

fun Instant.toDayTimeText(
    now: Instant,
    hourFormat: HourFormat,
    timeZone: TimeZone,
    languageTag: String
): String {
    val localDateTime: LocalDateTime = toLocalDateTime(timeZone = timeZone)
    val today: LocalDate = now.toLocalDateTime(timeZone = timeZone).date
    val timeText: String = toTimeText(
        hourFormat = hourFormat,
        timeZone = timeZone
    )
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