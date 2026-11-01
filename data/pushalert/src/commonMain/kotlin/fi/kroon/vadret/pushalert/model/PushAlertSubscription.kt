package fi.kroon.vadret.pushalert.model

import fi.kroon.vadret.geohash.model.Geohash

data class PushAlertSubscription(
    val geohash: Geohash,
    val warningLowestLevel: WarningAlertLevel?,
    val pollenChoices: List<PollenChoice>,
    val temperatureTrend: Boolean
)