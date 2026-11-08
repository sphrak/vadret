package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pushalert.model.HourFormat
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
    hourFormat = HourFormat.entries.firstOrNull { knownHourFormat -> knownHourFormat.name == hourFormat } ?: HourFormat.TWENTY_FOUR_HOUR,
    isChosenPlaceNotified = isChosenPlaceNotified,
    notifiedFavoriteIds = notifiedFavoriteIds.toSet()
)

private fun PushAlertChoicesEntity.storedPollenLowestLevel(): Int? = pollenLowestLevel ?: pollenChoices.firstOrNull()?.lowestLevel