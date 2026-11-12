package se.vadretapp.vadret.forecastwarning.extension

import kotlinx.collections.immutable.ImmutableList
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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

internal fun WarningItem.withAuroraHours(
    auroraHours: List<AuroraHour>,
    now: Instant,
    timeZone: TimeZone,
    hourFormat: HourFormat
): WarningItem =
    if (auroraHours.isEmpty()) {
        this
    } else {
        val auroraHourItems: ImmutableList<AuroraHourItem> = auroraHours.map { auroraHour: AuroraHour ->
            AuroraHourItem(
                key = "$AURORA_HOUR_KEY_PREFIX${auroraHour.time}",
                isNow = now >= auroraHour.time && now < auroraHour.time + ONE_HOUR,
                hourText = auroraHour.time.toHourText(
                    hourFormat = hourFormat,
                    timeZone = timeZone
                ),
                score = auroraHour.score,
                level = auroraHour.score / MAXIMUM_SCORE
            )
        }.toImmutableList()
        val hourly = WarningDetail.Hourly(
            labelResource = Res.string.aurora_hours_label,
            auroraHourItems = auroraHourItems,
            firstShownHourIndex = auroraHourItems
                .indexOfFirst { auroraHourItem: AuroraHourItem -> auroraHourItem.isNow }
                .coerceAtLeast(minimumValue = FIRST_HOUR_INDEX)
        )
        copy(details = (listOf(hourly) + details).toImmutableList())
    }

private const val AURORA_HOUR_KEY_PREFIX = "aurora-hour-"
private const val MAXIMUM_SCORE = 100f
private const val FIRST_HOUR_INDEX = 0
private val ONE_HOUR = 1.hours