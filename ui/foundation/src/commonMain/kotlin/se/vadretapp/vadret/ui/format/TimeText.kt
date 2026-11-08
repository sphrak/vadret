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

// The 12 hour clock names the half of the day in the language shown, such as PM or em.
fun Instant.toTimeText(
    hourFormat: HourFormat,
    timeZone: TimeZone,
    languageTag: String
): String =
    when (hourFormat) {
        HourFormat.TWENTY_FOUR_HOUR -> toLocalDateTime(timeZone = timeZone).time.format(format = twentyFourHourTimeFormat)
        HourFormat.TWELVE_HOUR -> DateTextFormatter(
            dateTextStyle = DateTextStyle.TWELVE_HOUR_TIME,
            languageTag = languageTag,
            timeZone = timeZone
        ).format(instant = this)
    }

fun Instant.toHourText(
    hourFormat: HourFormat,
    timeZone: TimeZone,
    languageTag: String
): String =
    when (hourFormat) {
        HourFormat.TWENTY_FOUR_HOUR -> toLocalDateTime(timeZone = timeZone).time.format(format = twentyFourHourFormat)
        HourFormat.TWELVE_HOUR -> DateTextFormatter(
            dateTextStyle = DateTextStyle.TWELVE_HOUR,
            languageTag = languageTag,
            timeZone = timeZone
        ).format(instant = this)
    }