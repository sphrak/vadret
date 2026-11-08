package se.vadretapp.vadret.ui.format

import java.text.DateFormatSymbols
import java.util.Locale

private const val BEFORE_NOON_INDEX = 0
private const val AFTER_NOON_INDEX = 1

actual fun DayPeriods(languageTag: String): DayPeriods {
    val dayPeriodNames: Array<String> = DateFormatSymbols.getInstance(Locale.forLanguageTag(languageTag)).amPmStrings
    return DayPeriods(
        beforeNoon = dayPeriodNames[BEFORE_NOON_INDEX],
        afterNoon = dayPeriodNames[AFTER_NOON_INDEX]
    )
}