package se.vadretapp.vadret.ui.format

import kotlinx.datetime.TimeZone
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Instant
import java.util.TimeZone as JavaTimeZone

private const val WEEKDAY_PATTERN = "EEEE"
private const val SHORT_WEEKDAY_PATTERN = "EEE"
private const val DAY_AND_SHORT_MONTH_PATTERN = "d MMM"

actual fun DateTextFormatter(
    dateTextStyle: DateTextStyle,
    languageTag: String,
    timeZone: TimeZone
): DateTextFormatter {
    val locale: Locale = Locale.forLanguageTag(languageTag)
    val dateFormat: DateFormat = when (dateTextStyle) {
        DateTextStyle.WEEKDAY -> SimpleDateFormat(WEEKDAY_PATTERN, locale)
        DateTextStyle.SHORT_WEEKDAY -> SimpleDateFormat(SHORT_WEEKDAY_PATTERN, locale)
        DateTextStyle.DAY_AND_SHORT_MONTH -> SimpleDateFormat(DAY_AND_SHORT_MONTH_PATTERN, locale)
        DateTextStyle.LONG_DATE -> DateFormat.getDateInstance(DateFormat.LONG, locale)
    }
    dateFormat.timeZone = JavaTimeZone.getTimeZone(timeZone.id)
    return DefaultDateTextFormatter(dateFormat = dateFormat)
}

internal class DefaultDateTextFormatter(
    private val dateFormat: DateFormat
) : DateTextFormatter {

    override fun format(instant: Instant): String = dateFormat.format(Date(instant.toEpochMilliseconds()))
}