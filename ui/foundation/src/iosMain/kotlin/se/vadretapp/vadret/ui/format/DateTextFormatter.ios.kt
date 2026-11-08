package se.vadretapp.vadret.ui.format

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toNSDate
import kotlinx.datetime.toNSTimeZone
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterLongStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSLocale
import kotlin.time.Instant

private const val WEEKDAY_PATTERN = "EEEE"
private const val SHORT_WEEKDAY_PATTERN = "EEE"
private const val DAY_AND_SHORT_MONTH_PATTERN = "d MMM"
private const val TWELVE_HOUR_TIME_PATTERN = "h:mm a"
private const val TWELVE_HOUR_PATTERN = "h a"

actual fun DateTextFormatter(
    dateTextStyle: DateTextStyle,
    languageTag: String,
    timeZone: TimeZone
): DateTextFormatter {
    val dateFormatter = NSDateFormatter()
    dateFormatter.locale = NSLocale(localeIdentifier = languageTag)
    dateFormatter.timeZone = timeZone.toNSTimeZone()
    when (dateTextStyle) {
        DateTextStyle.WEEKDAY -> dateFormatter.dateFormat = WEEKDAY_PATTERN
        DateTextStyle.SHORT_WEEKDAY -> dateFormatter.dateFormat = SHORT_WEEKDAY_PATTERN
        DateTextStyle.DAY_AND_SHORT_MONTH -> dateFormatter.dateFormat = DAY_AND_SHORT_MONTH_PATTERN
        DateTextStyle.TWELVE_HOUR_TIME -> dateFormatter.dateFormat = TWELVE_HOUR_TIME_PATTERN
        DateTextStyle.TWELVE_HOUR -> dateFormatter.dateFormat = TWELVE_HOUR_PATTERN
        DateTextStyle.LONG_DATE -> {
            dateFormatter.dateStyle = NSDateFormatterLongStyle
            dateFormatter.timeStyle = NSDateFormatterNoStyle
        }
    }
    return DefaultDateTextFormatter(dateFormatter = dateFormatter)
}

internal class DefaultDateTextFormatter(
    private val dateFormatter: NSDateFormatter
) : DateTextFormatter {

    override fun format(instant: Instant): String = dateFormatter.stringFromDate(date = instant.toNSDate())
}