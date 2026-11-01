package fi.kroon.vadret.pushalert.extension

import fi.kroon.vadret.pushalert.model.PollenChoiceEntity
import fi.kroon.vadret.pushalert.model.PushAlertChoices
import fi.kroon.vadret.pushalert.model.PushAlertChoicesEntity

internal fun PushAlertChoices.toPushAlertChoicesEntity(): PushAlertChoicesEntity = PushAlertChoicesEntity(
    warningLowestLevel = warningLowestLevel?.name,
    pollenChoices = pollenChoices.map { pollenChoice ->
        PollenChoiceEntity(
            pollenTypeId = pollenChoice.pollenTypeId,
            lowestLevel = pollenChoice.lowestLevel.level
        )
    },
    temperatureTrend = temperatureTrend
)