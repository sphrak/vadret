package se.vadretapp.vadret.pollen.model

import kotlinx.datetime.LocalDate

data class PollenForecast(
    val forecastId: String,
    val regionId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val text: String,
    val isEndOfSeason: Boolean,
    val dailyLevels: List<DailyPollenLevel>
)