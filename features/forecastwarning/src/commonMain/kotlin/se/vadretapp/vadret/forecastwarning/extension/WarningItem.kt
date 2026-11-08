package se.vadretapp.vadret.forecastwarning.extension

import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.aurora.model.AuroraHour
import se.vadretapp.vadret.forecastwarning.model.AuroraHourItem
import se.vadretapp.vadret.forecastwarning.model.WarningDetail
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.format.toHourText
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.aurora_hours_label

internal fun WarningItem.withAuroraHours(
    auroraHours: List<AuroraHour>,
    timeZone: TimeZone,
    languageTag: String,
    hourFormat: HourFormat
): WarningItem =
    if (auroraHours.isEmpty()) {
        this
    } else {
        val hourly = WarningDetail.Hourly(
            labelResource = Res.string.aurora_hours_label,
            auroraHourItems = auroraHours.map { auroraHour: AuroraHour ->
                AuroraHourItem(
                    hourText = auroraHour.time.toHourText(
                        hourFormat = hourFormat,
                        timeZone = timeZone,
                        languageTag = languageTag
                    ),
                    score = auroraHour.score
                )
            }.toImmutableList()
        )
        copy(details = (listOf(hourly) + details).toImmutableList())
    }