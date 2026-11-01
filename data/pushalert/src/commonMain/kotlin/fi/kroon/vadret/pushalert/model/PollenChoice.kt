package fi.kroon.vadret.pushalert.model

data class PollenChoice(
    val pollenTypeId: String,
    val lowestLevel: PollenAlertLevel
)