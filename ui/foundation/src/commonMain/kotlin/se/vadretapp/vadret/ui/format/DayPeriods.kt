package se.vadretapp.vadret.ui.format

data class DayPeriods(
    val beforeNoon: String,
    val afterNoon: String
)

expect fun DayPeriods(languageTag: String): DayPeriods