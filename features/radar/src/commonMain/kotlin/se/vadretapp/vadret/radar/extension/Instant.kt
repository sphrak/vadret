package se.vadretapp.vadret.radar.extension

import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.ui.format.DateTextFormatter
import se.vadretapp.vadret.ui.format.DateTextStyle

internal fun weekdayFormatter(
    timeZone: TimeZone,
    languageTag: String
): DateTextFormatter =
    DateTextFormatter(
        dateTextStyle = DateTextStyle.WEEKDAY,
        languageTag = languageTag,
        timeZone = timeZone
    )