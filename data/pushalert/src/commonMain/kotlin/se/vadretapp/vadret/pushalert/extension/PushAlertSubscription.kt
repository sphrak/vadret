package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pushalert.model.PollenSubscriptionDto
import se.vadretapp.vadret.pushalert.model.PushAlertSubscription
import se.vadretapp.vadret.pushalert.model.SubscriptionDto

internal fun PushAlertSubscription?.toSubscriptionDto(): SubscriptionDto =
    if (this == null) {
        SubscriptionDto()
    } else {
        SubscriptionDto(
            geohashes = geohashes.map { geohash -> geohash.value },
            warningLowestLevel = warningLowestLevel?.name,
            pollen = pollenChoices.map { pollenChoice ->
                PollenSubscriptionDto(
                    pollenTypeId = pollenChoice.pollenTypeId,
                    lowestLevel = pollenChoice.lowestLevel.level
                )
            },
            temperatureTrend = temperatureTrend,
            strongWind = strongWind
        )
    }