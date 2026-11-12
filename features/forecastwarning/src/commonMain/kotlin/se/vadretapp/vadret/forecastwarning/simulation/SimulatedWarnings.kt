package se.vadretapp.vadret.forecastwarning.simulation

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.Aurora
import se.vadretapp.vadret.aggregatedfeed.model.AuroraChance
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
import se.vadretapp.vadret.geohash.model.Geohash
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private const val HIGH_AURORA_SCORE = 78
private const val POSSIBLE_AURORA_SCORE = 34

private val stockholmCell = Geohash(value = "u6sce")
private val kirunaCell = Geohash(value = "ugezs")

private val noArea = FeedArea(
    polygons = emptyList(),
    lines = emptyList(),
    regions = emptyList()
)

// One warning of each kind the app can show, timed around now so every story reads as it would tonight. They are made
// up and never leave the developer settings.
internal fun simulatedWarnings(now: Instant): List<AggregatedFeed> =
    listOf(
        auroraWarning(
            identifier = "simulated-aurora-high",
            chance = AuroraChance.HIGH,
            score = HIGH_AURORA_SCORE,
            geohash = kirunaCell,
            now = now
        ),
        auroraWarning(
            identifier = "simulated-aurora-possible",
            chance = AuroraChance.POSSIBLE,
            score = POSSIBLE_AURORA_SCORE,
            geohash = stockholmCell,
            now = now
        ),
        AggregatedFeed(
            identifier = "simulated-slipperiness",
            headline = "",
            areaName = null,
            warningLevel = null,
            details = emptyList(),
            source = FeedSource.VADRET_API,
            isPublicAlert = false,
            senderName = null,
            publishedAt = now - 40.minutes,
            startsAt = now + 3.hours,
            endsAt = now + 10.hours,
            area = rectangleArea(
                south = 59.326171875,
                west = 18.0615234375,
                north = 59.3701171875,
                east = 18.10546875
            ),
            slipperiness = Slipperiness(
                risk = SlipperinessRisk.HIGH,
                causes = listOf(SlipperinessCause.FREEZING_RAIN, SlipperinessCause.FROST)
            ),
            aurora = null
        ),
        smhiWarning(
            identifier = "simulated-smhi-yellow",
            headline = "Vind",
            areaName = "Gotlands län",
            warningLevel = WarningLevel.YELLOW,
            text = "Sydvästlig vind med medelvind 15 m/s och byar upp till 25 m/s längs kusten.",
            area = rectangleArea(
                south = 56.9,
                west = 18.1,
                north = 57.95,
                east = 19.35
            ),
            now = now
        ),
        smhiWarning(
            identifier = "simulated-smhi-orange",
            headline = "Snöfall",
            areaName = "Norrbottens län",
            warningLevel = WarningLevel.ORANGE,
            text = "Snöfall som ger 25-35 cm snö på ett dygn, i kombination med hård vind.",
            area = rectangleArea(
                south = 65.8,
                west = 18.5,
                north = 68.4,
                east = 23.6
            ),
            now = now
        ),
        smhiWarning(
            identifier = "simulated-smhi-red",
            headline = "Mycket höga flöden",
            areaName = "Västra Götalands län",
            warningLevel = WarningLevel.RED,
            text = "Mycket höga flöden i vattendrag. Översvämningar av vägar och bebyggelse väntas.",
            area = rectangleArea(
                south = 57.6,
                west = 11.8,
                north = 58.6,
                east = 13.2
            ),
            now = now
        ),
        AggregatedFeed(
            identifier = "simulated-public-alert",
            headline = "Viktigt meddelande till allmänheten",
            areaName = null,
            warningLevel = null,
            details = listOf(
                FeedDetail(
                    kind = FeedDetailKind.BODY,
                    title = "",
                    text = "Brand i industribyggnad. Håll dörrar och fönster stängda och stäng av ventilationen."
                )
            ),
            source = FeedSource.KRISINFORMATION,
            isPublicAlert = true,
            senderName = "Räddningstjänsten Storstockholm",
            publishedAt = now - 10.minutes,
            startsAt = null,
            endsAt = null,
            area = FeedArea(
                polygons = emptyList(),
                lines = emptyList(),
                regions = listOf(
                    FeedRegion(
                        kind = FeedRegionKind.MUNICIPALITY,
                        name = "Stockholm"
                    )
                )
            ),
            slipperiness = null,
            aurora = null
        )
    )

private fun auroraWarning(
    identifier: String,
    chance: AuroraChance,
    score: Int,
    geohash: Geohash,
    now: Instant
): AggregatedFeed =
    AggregatedFeed(
        identifier = identifier,
        headline = "",
        areaName = null,
        warningLevel = null,
        details = emptyList(),
        source = FeedSource.VADRET_API,
        isPublicAlert = false,
        senderName = null,
        publishedAt = now - 20.minutes,
        startsAt = now + 1.hours,
        endsAt = now + 6.hours,
        area = noArea,
        slipperiness = null,
        aurora = Aurora(
            chance = chance,
            score = score,
            peakAt = now + 3.hours,
            geohash = geohash,
            auroraNight = now.toLocalDateTime(timeZone = TimeZone.currentSystemDefault()).date.toString()
        )
    )

private fun smhiWarning(
    identifier: String,
    headline: String,
    areaName: String,
    warningLevel: WarningLevel,
    text: String,
    area: FeedArea,
    now: Instant
): AggregatedFeed =
    AggregatedFeed(
        identifier = identifier,
        headline = headline,
        areaName = areaName,
        warningLevel = warningLevel,
        details = listOf(
            FeedDetail(
                kind = FeedDetailKind.WHAT_HAPPENS,
                title = "Vad händer",
                text = text
            )
        ),
        source = FeedSource.SMHI,
        isPublicAlert = false,
        senderName = null,
        publishedAt = now - 2.hours,
        startsAt = now + 2.hours,
        endsAt = now + 20.hours,
        area = area,
        slipperiness = null,
        aurora = null
    )

private fun rectangleArea(
    south: Double,
    west: Double,
    north: Double,
    east: Double
): FeedArea =
    FeedArea(
        polygons = listOf(
            GeoPolygon(
                outline = listOf(
                    GeoPosition(latitude = south, longitude = west),
                    GeoPosition(latitude = south, longitude = east),
                    GeoPosition(latitude = north, longitude = east),
                    GeoPosition(latitude = north, longitude = west),
                    GeoPosition(latitude = south, longitude = west)
                ),
                holes = emptyList()
            )
        ),
        lines = emptyList(),
        regions = emptyList()
    )