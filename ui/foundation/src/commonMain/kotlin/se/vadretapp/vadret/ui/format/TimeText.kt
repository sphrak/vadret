package se.vadretapp.vadret.ui.format

import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import se.vadretapp.vadret.setting.model.HourFormat
import kotlin.time.Instant

private val twentyFourHourTimeFormat = LocalTime.Format {
    hour()
    char(value = ':')
    minute()
}

private val twentyFourHourFormat = LocalTime.Format {
    hour()
}

// The 12 hour clock is written here rather than by a platform pattern, since iOS turns such a pattern back into the
// 24 hour clock on a phone set to it.
fun Instant.toTimeText(
    hourFormat: HourFormat,
    timeZone: TimeZone
): String {
    val localTime: LocalTime = toLocalDateTime(timeZone = timeZone).time
    return when (hourFormat) {
        HourFormat.TWENTY_FOUR_HOUR -> localTime.format(format = twentyFourHourTimeFormat)
        HourFormat.TWELVE_HOUR -> "${localTime.hour.toTwelveHourClockHour()}$TIME_SEPARATOR${localTime.minute.toTwoDigits()} ${localTime.hour.halfOfDay()}"
    }
}

fun Instant.toHourText(
    hourFormat: HourFormat,
    timeZone: TimeZone
): String {
    val localTime: LocalTime = toLocalDateTime(timeZone = timeZone).time
    return when (hourFormat) {
        HourFormat.TWENTY_FOUR_HOUR -> localTime.format(format = twentyFourHourFormat)
        HourFormat.TWELVE_HOUR -> "${localTime.hour.toTwelveHourClockHour()} ${localTime.hour.halfOfDay()}"
    }
}

private fun Int.halfOfDay(): String = if (this < HOURS_PER_HALF_DAY) {
    BEFORE_NOON
} else {
    AFTER_NOON
}

private fun Int.toTwelveHourClockHour(): Int {
    val hourOfHalfDay: Int = this % HOURS_PER_HALF_DAY
    return if (hourOfHalfDay == 0) {
        HOURS_PER_HALF_DAY
    } else {
        hourOfHalfDay
    }
}

private fun Int.toTwoDigits(): String = toString().padStart(
    length = MINUTE_DIGITS,
    padChar = '0'
)

private const val HOURS_PER_HALF_DAY = 12
private const val MINUTE_DIGITS = 2
private const val TIME_SEPARATOR = ':'
private const val BEFORE_NOON = "AM"
private const val AFTER_NOON = "PM"