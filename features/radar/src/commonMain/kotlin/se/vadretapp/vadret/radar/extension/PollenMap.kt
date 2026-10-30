package se.vadretapp.vadret.radar.extension

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import se.vadretapp.vadret.pollen.model.PollenLevel
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.radar.model.PollenMap
import se.vadretapp.vadret.radar.model.PollenMarker
import se.vadretapp.vadret.radar.model.PollenStation
import se.vadretapp.vadret.ui.format.DateTextFormatter
import se.vadretapp.vadret.ui.format.DateTextStyle

internal fun PollenMap.strongestPollenId(): String? =
    pollenTypes
        .maxByOrNull { pollenType: PollenType -> strongestLevel(pollenId = pollenType.pollenId) }
        ?.pollenId

internal fun PollenMap.markersFor(pollenId: String?): ImmutableList<PollenMarker> =
    stations
        .mapNotNull { station: PollenStation -> station.markerFor(pollenId = pollenId) }
        .toImmutableList()

internal fun PollenMap.toDayText(
    timeZone: TimeZone,
    languageTag: String
): String =
    weekdayFormatter(
        timeZone = timeZone,
        languageTag = languageTag
    ).format(instant = date.atStartOfDayIn(timeZone = timeZone))
        .replaceFirstChar { character: Char -> character.titlecase() }

internal fun PollenMap.toDateText(
    timeZone: TimeZone,
    languageTag: String
): String =
    DateTextFormatter(
        dateTextStyle = DateTextStyle.LONG_DATE,
        languageTag = languageTag,
        timeZone = timeZone
    ).format(instant = date.atStartOfDayIn(timeZone = timeZone))

private fun PollenMap.strongestLevel(pollenId: String): PollenLevel =
    stations
        .mapNotNull { station: PollenStation -> station.pollenLevels[pollenId] }
        .maxOrNull() ?: PollenLevel.NONE

private fun PollenStation.markerFor(pollenId: String?): PollenMarker? {
    val pollenLevel: PollenLevel? = if (pollenId != null) {
        pollenLevels[pollenId]
    } else {
        null
    }
    return if (pollenLevel != null) {
        PollenMarker(
            latitude = latitude,
            longitude = longitude,
            pollenLevel = pollenLevel
        )
    } else {
        null
    }
}