package se.vadretapp.vadret.ui.format

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale

actual fun DayPeriods(languageTag: String): DayPeriods {
    val dateFormatter = NSDateFormatter()
    dateFormatter.locale = NSLocale(localeIdentifier = languageTag)
    return DayPeriods(
        beforeNoon = dateFormatter.AMSymbol,
        afterNoon = dateFormatter.PMSymbol
    )
}