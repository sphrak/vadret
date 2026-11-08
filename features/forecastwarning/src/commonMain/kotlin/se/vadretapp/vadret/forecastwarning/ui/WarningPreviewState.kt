package se.vadretapp.vadret.forecastwarning.ui

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.FeedArea
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetail
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetailKind
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegion
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegionKind
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.GeoPolygon
import se.vadretapp.vadret.aggregatedfeed.model.GeoPosition
import se.vadretapp.vadret.aggregatedfeed.model.Slipperiness
import se.vadretapp.vadret.aggregatedfeed.model.SlipperinessCause
import se.vadretapp.vadret.aggregatedfeed.model.SlipperinessRisk
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel
import se.vadretapp.vadret.forecastwarning.WarningState
import se.vadretapp.vadret.forecastwarning.extension.toWarningItem
import se.vadretapp.vadret.forecastwarning.extension.toWarningItems
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private const val PREVIEW_LANGUAGE_TAG = "sv-SE"

private val previewNow: Instant = Instant.parse(input = "2026-10-24T09:00:00Z")
private val previewTimeZone: TimeZone = TimeZone.of(zoneId = "Europe/Stockholm")

private fun square(
    south: Double,
    west: Double,
    north: Double,
    east: Double
): GeoPolygon =
    GeoPolygon(
        outline = listOf(
            GeoPosition(
                latitude = south,
                longitude = west
            ),
            GeoPosition(
                latitude = south,
                longitude = east
            ),
            GeoPosition(
                latitude = north,
                longitude = east
            ),
            GeoPosition(
                latitude = north,
                longitude = west
            ),
            GeoPosition(
                latitude = south,
                longitude = west
            )
        ),
        holes = emptyList()
    )

private fun smhiWarning(
    identifier: String,
    headline: String,
    areaName: String,
    warningLevel: WarningLevel,
    hoursAgo: Int,
    startsAt: Instant?,
    endsAt: Instant?,
    polygon: GeoPolygon,
    description: String
): AggregatedFeed =
    AggregatedFeed(
        identifier = identifier,
        headline = headline,
        areaName = areaName,
        warningLevel = warningLevel,
        details = listOf(
            FeedDetail(
                kind = FeedDetailKind.DESCRIPTION,
                title = "Händelsebeskrivning",
                text = description
            ),
            FeedDetail(
                kind = FeedDetailKind.ADVICE,
                title = "Vad ska jag tänka på?",
                text = "Undvik onödiga resor och följ myndigheternas råd."
            )
        ),
        source = FeedSource.SMHI,
        isPublicAlert = false,
        senderName = null,
        publishedAt = previewNow - hoursAgo.hours,
        startsAt = startsAt,
        endsAt = endsAt,
        area = FeedArea(
            polygons = listOf(polygon),
            lines = emptyList(),
            regions = emptyList()
        ),
        slipperiness = null
    )

private val previewFeeds: List<AggregatedFeed> = listOf(
    smhiWarning(
        identifier = "smhi-yellow",
        headline = "Hård vind med snödrev eller nederbörd",
        areaName = "Härjedalsfjällen, Jämtlands län",
        warningLevel = WarningLevel.YELLOW,
        hoursAgo = 12,
        startsAt = null,
        endsAt = previewNow + 12.hours,
        polygon = square(
            south = 62.2,
            west = 12.2,
            north = 62.8,
            east = 13.4
        ),
        description = "Täta snöbyar och på kalfjället tidvis hård nordvästlig till nordlig vind."
    ),
    smhiWarning(
        identifier = "smhi-orange",
        headline = "Mycket kraftiga vindbyar",
        areaName = "Gotlands län",
        warningLevel = WarningLevel.ORANGE,
        hoursAgo = 3,
        startsAt = previewNow + 8.hours,
        endsAt = previewNow + 30.hours,
        polygon = square(
            south = 56.9,
            west = 18.1,
            north = 57.9,
            east = 19.2
        ),
        description = "Vindbyar på 30 m/s, under natten mot söndag."
    ),
    smhiWarning(
        identifier = "smhi-red",
        headline = "Extremt kraftiga regnskurar",
        areaName = "Västra Götalands län",
        warningLevel = WarningLevel.RED,
        hoursAgo = 1,
        startsAt = previewNow - 1.hours,
        endsAt = previewNow + 6.hours,
        polygon = square(
            south = 57.5,
            west = 11.7,
            north = 58.4,
            east = 12.8
        ),
        description = "Skyfall med upp till 80 mm på några timmar."
    ),
    AggregatedFeed(
        identifier = "krisinformation-fjallen",
        headline = "Undvik färd på kalfjället i helgen",
        areaName = null,
        warningLevel = null,
        details = listOf(
            FeedDetail(
                kind = FeedDetailKind.SUMMARY,
                title = "",
                text = "Länsstyrelsen avråder från färd på kalfjället under helgen på grund av hård vind och snödrev."
            )
        ),
        source = FeedSource.KRISINFORMATION,
        isPublicAlert = false,
        senderName = "Länsstyrelsen Jämtlands län",
        publishedAt = previewNow - 3.hours,
        startsAt = null,
        endsAt = null,
        area = FeedArea(
            polygons = emptyList(),
            lines = emptyList(),
            regions = listOf(
                FeedRegion(
                    kind = FeedRegionKind.COUNTY,
                    name = "Jämtlands län"
                )
            )
        ),
        slipperiness = null
    ),
    AggregatedFeed(
        identifier = "krisinformation-vma-ostersund",
        headline = "Viktigt meddelande till allmänheten: brand i Östersund",
        areaName = null,
        warningLevel = null,
        details = listOf(
            FeedDetail(
                kind = FeedDetailKind.SUMMARY,
                title = "",
                text = "Räddningstjänsten uppmanar alla i centrala Östersund att gå inomhus och stänga dörrar, fönster och ventilation."
            )
        ),
        source = FeedSource.KRISINFORMATION,
        isPublicAlert = true,
        senderName = "Räddningstjänsten Jämtland",
        publishedAt = previewNow - 1.hours,
        startsAt = null,
        endsAt = null,
        area = FeedArea(
            polygons = emptyList(),
            lines = emptyList(),
            regions = listOf(
                FeedRegion(
                    kind = FeedRegionKind.COUNTY,
                    name = "Jämtlands län"
                )
            )
        ),
        slipperiness = null
    ),
    AggregatedFeed(
        identifier = "vadret-slipperiness-u6sce-2026-10-24-HIGH",
        headline = "",
        areaName = null,
        warningLevel = null,
        details = emptyList(),
        source = FeedSource.VADRET_API,
        isPublicAlert = false,
        senderName = null,
        publishedAt = previewNow,
        startsAt = previewNow + 13.hours,
        endsAt = previewNow + 22.hours,
        area = FeedArea(
            polygons = listOf(
                square(
                    south = 59.326171875,
                    west = 18.0615234375,
                    north = 59.3701171875,
                    east = 18.10546875
                )
            ),
            lines = emptyList(),
            regions = emptyList()
        ),
        slipperiness = Slipperiness(
            risk = SlipperinessRisk.HIGH,
            causes = listOf(
                SlipperinessCause.REFREEZE,
                SlipperinessCause.FROST
            )
        )
    )
)

internal fun warningPreviewState(): WarningState =
    WarningState(
        warnings = previewFeeds.toWarningItems(
            now = previewNow,
            timeZone = previewTimeZone,
            languageTag = PREVIEW_LANGUAGE_TAG
        ),
        isLoading = false
    )

internal class WarningPreviewParameterProvider : PreviewParameterProvider<WarningItem> {
    override val values: Sequence<WarningItem> = previewFeeds
        .asSequence()
        .map { aggregatedFeed: AggregatedFeed ->
            aggregatedFeed.toWarningItem(
                now = previewNow,
                timeZone = previewTimeZone,
                languageTag = PREVIEW_LANGUAGE_TAG
            )
        }
}