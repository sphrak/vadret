package fi.kroon.vadret.pushalert.extension

import fi.kroon.vadret.pushalert.model.PollenSubscriptionDto
import fi.kroon.vadret.pushalert.model.PushAlertSubscription
import fi.kroon.vadret.pushalert.model.SubscriptionDto

internal fun PushAlertSubscription?.toSubscriptionDto(): SubscriptionDto =
    if (this == null) {
        SubscriptionDto()
    } else {
        SubscriptionDto(
            geohash = geohash.value,
            warningLowestLevel = warningLowestLevel?.name,
            pollen = pollenChoices.map { pollenChoice ->
                PollenSubscriptionDto(
                    pollenTypeId = pollenChoice.pollenTypeId,
                    lowestLevel = pollenChoice.lowestLevel.level
                )
            },
            temperatureTrend = temperatureTrend
        )
    }