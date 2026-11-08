package se.vadretapp.vadret.pushalert.model

data class PushAlertChoices(
    val warningLowestLevel: WarningAlertLevel?,
    val pollenLowestLevel: PollenAlertLevel?,
    val temperatureTrend: Boolean,
    val strongWind: Boolean,
    val slipperiness: Boolean,
    val isChosenPlaceNotified: Boolean,
    val notifiedFavoriteIds: Set<String>
) {
    fun isAnythingChosen(): Boolean =
        warningLowestLevel != null ||
            pollenLowestLevel != null ||
            temperatureTrend ||
            strongWind ||
            slipperiness

    fun isWithin(other: PushAlertChoices): Boolean =
        (warningLowestLevel == null || warningLowestLevel == other.warningLowestLevel) &&
            (pollenLowestLevel == null || pollenLowestLevel == other.pollenLowestLevel) &&
            (!temperatureTrend || other.temperatureTrend) &&
            (!strongWind || other.strongWind) &&
            (!slipperiness || other.slipperiness)

    companion object {
        val NOTHING_CHOSEN: PushAlertChoices = PushAlertChoices(
            warningLowestLevel = null,
            pollenLowestLevel = null,
            temperatureTrend = false,
            strongWind = false,
            slipperiness = false,
            isChosenPlaceNotified = true,
            notifiedFavoriteIds = emptySet()
        )
    }
}