package fi.kroon.vadret.pollen.model

import kotlinx.datetime.LocalDate

data class DailyPollenLevel(
    val pollenId: String,
    val date: LocalDate,
    val pollenLevel: PollenLevel
)