package se.vadretapp.vadret.pushalert.model

import se.vadretapp.vadret.geohash.model.Geohash

data class PushAlertSubscription(
    val geohash: Geohash,
    val warningLowestLevel: WarningAlertLevel?,
    val pollenChoices: List<PollenChoice>,
    val temperatureTrend: Boolean
)