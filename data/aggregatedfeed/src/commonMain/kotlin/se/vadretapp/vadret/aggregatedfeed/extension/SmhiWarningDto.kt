package se.vadretapp.vadret.aggregatedfeed.extension

import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedEntity
import se.vadretapp.vadret.aggregatedfeed.local.FeedDetailEntity
import se.vadretapp.vadret.aggregatedfeed.local.FeedRegionEntity
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetailKind
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegionKind
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel
import se.vadretapp.vadret.aggregatedfeed.remote.GeoJsonDto
import se.vadretapp.vadret.aggregatedfeed.remote.SmhiAffectedAreaDto
import se.vadretapp.vadret.aggregatedfeed.remote.SmhiDescriptionDto
import se.vadretapp.vadret.aggregatedfeed.remote.SmhiWarningAreaDto
import se.vadretapp.vadret.aggregatedfeed.remote.SmhiWarningDto
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import kotlin.time.Instant

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.aggregatedfeed")

private val countyIds: IntRange = 1..25

private val seaDistrictIds: IntRange = 41..58

private val detailKindsByCode: Map<String, FeedDetailKind> = mapOf(
    "HAPPENS" to FeedDetailKind.WHAT_HAPPENS,
    "INCIDENT" to FeedDetailKind.DESCRIPTION,
    "WHERE" to FeedDetailKind.WHERE,
    "AFFECT" to FeedDetailKind.ADVICE,
    "COMMENTS" to FeedDetailKind.COMMENT
)

internal fun SmhiWarningDto.toAggregatedFeedEntities(): List<AggregatedFeedEntity> =
    warningAreas
        .orEmpty()
        .mapNotNull { smhiWarningAreaDto: SmhiWarningAreaDto -> toAggregatedFeedEntity(smhiWarningAreaDto = smhiWarningAreaDto) }

private fun SmhiWarningDto.toAggregatedFeedEntity(smhiWarningAreaDto: SmhiWarningAreaDto): AggregatedFeedEntity? {
    val publishedAt: Instant? = Instant.parseOrNull(input = smhiWarningAreaDto.published)
    if (publishedAt == null) {
        logger.warn { "Skipping SMHI warning area ${smhiWarningAreaDto.id}, published ${smhiWarningAreaDto.published}" }
        return null
    }
    val area: GeoJsonDto? = smhiWarningAreaDto.area
    if (area != null) {
        logUnsupportedGeometry(
            warningAreaId = smhiWarningAreaDto.id,
            area = area
        )
    }
    return AggregatedFeedEntity(
        identifier = "smhi-$id-${smhiWarningAreaDto.id}",
        headline = smhiWarningAreaDto.eventDescription.sv,
        areaName = smhiWarningAreaDto.areaName.sv,
        warningLevel = smhiWarningAreaDto.toWarningLevel()?.name,
        details = (smhiWarningAreaDto.descriptions.orEmpty() + descriptions.orEmpty())
            .map { smhiDescriptionDto: SmhiDescriptionDto -> smhiDescriptionDto.toFeedDetailEntity() },
        source = FeedSource.SMHI.name,
        publishedAtEpochMillis = publishedAt.toEpochMilliseconds(),
        startsAtEpochMillis = smhiWarningAreaDto.approximateStart.toEpochMillisecondsOrNull(),
        endsAtEpochMillis = smhiWarningAreaDto.approximateEnd.toEpochMillisecondsOrNull(),
        polygons = area?.toGeoPolygonEntities().orEmpty(),
        lines = area?.toGeoLineEntities().orEmpty(),
        regions = smhiWarningAreaDto.affectedAreas
            .orEmpty()
            .mapNotNull { smhiAffectedAreaDto: SmhiAffectedAreaDto -> smhiAffectedAreaDto.toFeedRegionEntity() }
    )
}

private fun logUnsupportedGeometry(
    warningAreaId: Int,
    area: GeoJsonDto
) {
    val unsupportedGeometryTypes: Set<String> = area.unsupportedGeometryTypes()
    if (unsupportedGeometryTypes.isNotEmpty()) {
        logger.warn { "Ignoring $unsupportedGeometryTypes geometry on SMHI warning area $warningAreaId" }
    }
}

private fun SmhiWarningAreaDto.toWarningLevel(): WarningLevel? {
    val warningLevel: WarningLevel? = WarningLevel.entries.firstOrNull { level: WarningLevel -> level.name == this.warningLevel.code }
    if (warningLevel == null) {
        logger.warn { "Unknown SMHI warning level ${this.warningLevel.code} on warning area $id" }
    }
    return warningLevel
}

private fun SmhiAffectedAreaDto.toFeedRegionEntity(): FeedRegionEntity? {
    val name: String = sv ?: return null
    val feedRegionKind: FeedRegionKind = when (id) {
        in countyIds -> FeedRegionKind.COUNTY
        in seaDistrictIds -> FeedRegionKind.SEA
        else -> FeedRegionKind.OTHER
    }
    return FeedRegionEntity(
        kind = feedRegionKind.name,
        name = name
    )
}

private fun SmhiDescriptionDto.toFeedDetailEntity(): FeedDetailEntity =
    FeedDetailEntity(
        kind = (detailKindsByCode[title.code] ?: FeedDetailKind.OTHER).name,
        title = title.sv,
        text = text.sv
    )

private fun String?.toEpochMillisecondsOrNull(): Long? =
    if (this == null) {
        null
    } else {
        Instant.parseOrNull(input = this)?.toEpochMilliseconds()
    }