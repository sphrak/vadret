package se.vadretapp.vadret.weatherforecast.extension

import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.icons.Slipperiness
import se.vadretapp.vadret.icons.VadretIcons
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.format.toDayTimeText
import se.vadretapp.vadret.weatherforecast.model.LatestWarning
import kotlin.time.Instant

internal fun List<AggregatedFeed>.toLatestWarning(
    now: Instant,
    hourFormat: HourFormat,
    timeZone: TimeZone,
    languageTag: String
): LatestWarning? {
    val latest: AggregatedFeed = firstOrNull() ?: return null
    return LatestWarning(
        warningLevel = latest.warningLevel,
        slipperinessRisk = latest.slipperiness?.risk,
        icon = if (latest.slipperiness == null) {
            null
        } else {
            VadretIcons.Alert.Slipperiness
        },
        source = latest.source,
        isPublicAlert = latest.isPublicAlert,
        headline = latest.headline,
        endsAtText = latest.endsAt?.toDayTimeText(
            now = now,
            hourFormat = hourFormat,
            timeZone = timeZone,
            languageTag = languageTag
        ),
        warningIdentifiers = map { aggregatedFeed: AggregatedFeed -> aggregatedFeed.identifier }.toImmutableList()
    )
}