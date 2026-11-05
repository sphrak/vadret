package se.vadretapp.vadret.pushalert.model

data class PushAlertChoices(
    val warningLowestLevel: WarningAlertLevel?,
    val pollenChoices: List<PollenChoice>,
    val temperatureTrend: Boolean,
    val strongWind: Boolean
) {
    fun isAnythingChosen(): Boolean =
        warningLowestLevel != null ||
            pollenChoices.isNotEmpty() ||
            temperatureTrend ||
            strongWind

    fun isWithin(other: PushAlertChoices): Boolean =
        (warningLowestLevel == null || warningLowestLevel == other.warningLowestLevel) &&
            (!temperatureTrend || other.temperatureTrend) &&
            (!strongWind || other.strongWind) &&
            other.pollenChoices.containsAll(elements = pollenChoices)

    companion object {
        val NOTHING_CHOSEN: PushAlertChoices = PushAlertChoices(
            warningLowestLevel = null,
            pollenChoices = emptyList(),
            temperatureTrend = false,
            strongWind = false
        )
    }
}