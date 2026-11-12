package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertChoicesEntity
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel

internal fun PushAlertChoicesEntity.toPushAlertChoices(): PushAlertChoices = PushAlertChoices(
    warningLowestLevel = WarningAlertLevel.entries.firstOrNull { warningAlertLevel -> warningAlertLevel.name == warningLowestLevel },
    pollenLowestLevel = PollenAlertLevel.entries.firstOrNull { pollenAlertLevel -> pollenAlertLevel.level == storedPollenLowestLevel() },
    temperatureTrend = temperatureTrend,
    strongWind = strongWind,
    slipperiness = slipperiness,
    aurora = aurora,
    dailyForecast = dailyForecast,
    isChosenPlaceNotified = isChosenPlaceNotified,
    notifiedFavoriteIds = notifiedFavoriteIds.toSet()
)

private fun PushAlertChoicesEntity.storedPollenLowestLevel(): Int? = pollenLowestLevel ?: pollenChoices.firstOrNull()?.lowestLevel