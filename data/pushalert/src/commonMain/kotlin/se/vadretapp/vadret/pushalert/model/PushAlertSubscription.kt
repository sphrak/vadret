package se.vadretapp.vadret.pushalert.model

import se.vadretapp.vadret.geohash.model.Geohash

data class PushAlertSubscription(
    val geohashes: List<Geohash>,
    val warningLowestLevel: WarningAlertLevel?,
    val pollenChoices: List<PollenChoice>,
    val temperatureTrend: Boolean,
    val strongWind: Boolean,
    val slipperiness: Boolean
)