package se.vadretapp.vadret.radar.extension

import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import se.vadretapp.vadret.ui.format.DateTextFormatter
import se.vadretapp.vadret.ui.format.DateTextStyle
import kotlin.time.Instant

private val timeFormat = LocalTime.Format {
    hour()
    char(value = ':')
    minute()
}

internal fun Instant.toTimeText(timeZone: TimeZone): String =
    toLocalDateTime(timeZone = timeZone)
        .time
        .format(format = timeFormat)

internal fun weekdayFormatter(
    timeZone: TimeZone,
    languageTag: String
): DateTextFormatter =
    DateTextFormatter(
        dateTextStyle = DateTextStyle.WEEKDAY,
        languageTag = languageTag,
        timeZone = timeZone
    )