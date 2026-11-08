package se.vadretapp.vadret.pushalert.model

import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormat

data class PushAlertSubscription(
    val geohashes: List<Geohash>,
    val warningLowestLevel: WarningAlertLevel?,
    val pollenChoices: List<PollenChoice>,
    val temperatureTrend: Boolean,
    val strongWind: Boolean,
    val slipperiness: Boolean,
    val hourFormat: HourFormat,
    val language: AppLanguage
)