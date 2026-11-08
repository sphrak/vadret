package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pushalert.model.PollenChoiceEntity
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertChoicesEntity

internal fun PushAlertChoices.toPushAlertChoicesEntity(): PushAlertChoicesEntity = PushAlertChoicesEntity(
    warningLowestLevel = warningLowestLevel?.name,
    pollenChoices = pollenChoices.map { pollenChoice ->
        PollenChoiceEntity(
            pollenTypeId = pollenChoice.pollenTypeId,
            lowestLevel = pollenChoice.lowestLevel.level
        )
    },
    temperatureTrend = temperatureTrend,
    strongWind = strongWind,
    slipperiness = slipperiness,
    isChosenPlaceNotified = isChosenPlaceNotified,
    notifiedFavoriteIds = notifiedFavoriteIds.sorted()
)