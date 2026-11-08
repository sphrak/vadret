package se.vadretapp.vadret.aggregatedfeed.extension

import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedEntity
import se.vadretapp.vadret.aggregatedfeed.local.FeedDetailEntity
import se.vadretapp.vadret.aggregatedfeed.local.FeedRegionEntity
import se.vadretapp.vadret.aggregatedfeed.local.GeoLineEntity
import se.vadretapp.vadret.aggregatedfeed.local.GeoPolygonEntity
import se.vadretapp.vadret.aggregatedfeed.local.GeoPositionEntity
import se.vadretapp.vadret.aggregatedfeed.local.SlipperinessEntity
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.FeedArea
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetail
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetailKind
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegion
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegionKind
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.GeoLine
import se.vadretapp.vadret.aggregatedfeed.model.GeoPolygon
import se.vadretapp.vadret.aggregatedfeed.model.GeoPosition
import se.vadretapp.vadret.aggregatedfeed.model.Slipperiness
import se.vadretapp.vadret.aggregatedfeed.model.SlipperinessCause
import se.vadretapp.vadret.aggregatedfeed.model.SlipperinessRisk
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import kotlin.time.Instant

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.aggregatedfeed")

internal fun AggregatedFeedEntity.toAggregatedFeed(): AggregatedFeed? {
    val feedSource: FeedSource? = FeedSource.entries.firstOrNull { feedSource: FeedSource -> feedSource.name == source }
    if (feedSource == null) {
        logger.warn { "Skipping cached item $identifier from unknown source $source" }
        return null
    }
    val slipperinessEntity: SlipperinessEntity? = slipperiness
    val cachedSlipperiness: Slipperiness? = slipperinessEntity?.toSlipperinessOrNull()
    if (slipperinessEntity != null && cachedSlipperiness == null) {
        logger.warn { "Skipping cached item $identifier with unknown slipperiness risk ${slipperinessEntity.risk}" }
        return null
    }
    return AggregatedFeed(
        identifier = identifier,
        headline = headline,
        areaName = areaName,
        warningLevel = WarningLevel.entries.firstOrNull { level: WarningLevel -> level.name == warningLevel },
        details = details.map { feedDetailEntity: FeedDetailEntity -> feedDetailEntity.toFeedDetail() },
        source = feedSource,
        isPublicAlert = isPublicAlert,
        senderName = senderName,
        publishedAt = Instant.fromEpochMilliseconds(epochMilliseconds = publishedAtEpochMillis),
        startsAt = startsAtEpochMillis.toInstantOrNull(),
        endsAt = endsAtEpochMillis.toInstantOrNull(),
        area = FeedArea(
            polygons = polygons.map { geoPolygonEntity: GeoPolygonEntity -> geoPolygonEntity.toGeoPolygon() },
            lines = lines.map { geoLineEntity: GeoLineEntity -> GeoLine(positions = geoLineEntity.positions.toGeoPositions()) },
            regions = regions.map { feedRegionEntity: FeedRegionEntity -> feedRegionEntity.toFeedRegion() }
        ),
        slipperiness = cachedSlipperiness
    )
}

private fun SlipperinessEntity.toSlipperinessOrNull(): Slipperiness? {
    val slipperinessRisk: SlipperinessRisk = SlipperinessRisk.entries.firstOrNull { slipperinessRisk: SlipperinessRisk -> slipperinessRisk.name == risk } ?: return null
    return Slipperiness(
        risk = slipperinessRisk,
        causes = causes.mapNotNull { cause: String -> SlipperinessCause.entries.firstOrNull { slipperinessCause: SlipperinessCause -> slipperinessCause.name == cause } }
    )
}

private fun FeedDetailEntity.toFeedDetail(): FeedDetail =
    FeedDetail(
        kind = FeedDetailKind.entries.firstOrNull { feedDetailKind: FeedDetailKind -> feedDetailKind.name == kind } ?: FeedDetailKind.OTHER,
        title = title,
        text = text
    )

private fun GeoPolygonEntity.toGeoPolygon(): GeoPolygon =
    GeoPolygon(
        outline = outline.toGeoPositions(),
        holes = holes.map { hole: List<GeoPositionEntity> -> hole.toGeoPositions() }
    )

private fun List<GeoPositionEntity>.toGeoPositions(): List<GeoPosition> =
    map { geoPositionEntity: GeoPositionEntity ->
        GeoPosition(
            latitude = geoPositionEntity.latitude,
            longitude = geoPositionEntity.longitude
        )
    }

private fun FeedRegionEntity.toFeedRegion(): FeedRegion =
    FeedRegion(
        kind = FeedRegionKind.entries.firstOrNull { feedRegionKind: FeedRegionKind -> feedRegionKind.name == kind } ?: FeedRegionKind.OTHER,
        name = name
    )

private fun Long?.toInstantOrNull(): Instant? =
    if (this == null) {
        null
    } else {
        Instant.fromEpochMilliseconds(epochMilliseconds = this)
    }