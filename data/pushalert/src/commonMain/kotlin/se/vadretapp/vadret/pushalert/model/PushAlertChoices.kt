package se.vadretapp.vadret.pushalert.model

data class PushAlertChoices(
    val warningLowestLevel: WarningAlertLevel?,
    val pollenChoices: List<PollenChoice>,
    val temperatureTrend: Boolean
) {
    fun isAnythingChosen(): Boolean = warningLowestLevel != null || pollenChoices.isNotEmpty() || temperatureTrend

    fun isWithin(other: PushAlertChoices): Boolean =
        (warningLowestLevel == null || warningLowestLevel == other.warningLowestLevel) &&
            (!temperatureTrend || other.temperatureTrend) &&
            other.pollenChoices.containsAll(elements = pollenChoices)

    companion object {
        val NOTHING_CHOSEN: PushAlertChoices = PushAlertChoices(
            warningLowestLevel = null,
            pollenChoices = emptyList(),
            temperatureTrend = false
        )
    }
}