package se.vadretapp.vadret.pushalert.model

import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.Language

data class PushAlertSubscription(
    val geohashes: List<Geohash>,
    val placeNames: Map<Geohash, PlaceName?>,
    val dailyGeohash: Geohash?,
    val warningLowestLevel: WarningAlertLevel?,
    val pollenChoices: List<PollenChoice>,
    val temperatureTrend: Boolean,
    val strongWind: Boolean,
    val slipperiness: Boolean,
    val aurora: Boolean,
    val hourFormat: HourFormat,
    val language: Language
)