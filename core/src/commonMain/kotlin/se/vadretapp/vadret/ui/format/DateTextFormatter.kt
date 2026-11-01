package se.vadretapp.vadret.ui.format

import kotlinx.datetime.TimeZone
import kotlin.time.Instant

interface DateTextFormatter {
    fun format(instant: Instant): String
}

expect fun DateTextFormatter(
    dateTextStyle: DateTextStyle,
    languageTag: String,
    timeZone: TimeZone
): DateTextFormatter