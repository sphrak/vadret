package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertChoicesEntity

internal fun PushAlertChoices.toPushAlertChoicesEntity(): PushAlertChoicesEntity = PushAlertChoicesEntity(
    warningLowestLevel = warningLowestLevel?.name,
    pollenLowestLevel = pollenLowestLevel?.level,
    temperatureTrend = temperatureTrend,
    strongWind = strongWind,
    slipperiness = slipperiness,
    hourFormat = hourFormat.name,
    isChosenPlaceNotified = isChosenPlaceNotified,
    notifiedFavoriteIds = notifiedFavoriteIds.sorted()
)