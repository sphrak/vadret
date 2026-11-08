package se.vadretapp.vadret.aggregatedfeed.extension

import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedEntity
import se.vadretapp.vadret.aggregatedfeed.local.AuroraEntity
import se.vadretapp.vadret.aggregatedfeed.local.SlipperinessEntity
import se.vadretapp.vadret.aggregatedfeed.model.AuroraChance
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.SlipperinessCause
import se.vadretapp.vadret.aggregatedfeed.model.SlipperinessRisk
import se.vadretapp.vadret.aggregatedfeed.remote.ForecastWarningAreaDto
import se.vadretapp.vadret.aggregatedfeed.remote.ForecastWarningCauseDto
import se.vadretapp.vadret.aggregatedfeed.remote.ForecastWarningDto
import se.vadretapp.vadret.aggregatedfeed.remote.ForecastWarningsDto
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.geohash.model.geohashOrNull
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import kotlin.time.Instant

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.aggregatedfeed")

private const val SLIPPERINESS_EVENT = "SLIPPERINESS"
private const val AURORA_EVENT = "AURORA"
private const val AREA_ID_SEPARATOR = "-"

internal fun ForecastWarningsDto.toAggregatedFeedEntities(): List<AggregatedFeedEntity> =
    warnings
        .orEmpty()
        .flatMap { forecastWarningDto: ForecastWarningDto -> forecastWarningDto.toAggregatedFeedEntities() }

private fun ForecastWarningDto.toAggregatedFeedEntities(): List<AggregatedFeedEntity> =
    when (event) {
        SLIPPERINESS_EVENT ->
            warningAreas
                .orEmpty()
                .mapNotNull { forecastWarningAreaDto: ForecastWarningAreaDto -> toAggregatedFeedEntity(forecastWarningAreaDto = forecastWarningAreaDto) }
        AURORA_EVENT ->
            warningAreas
                .orEmpty()
                .mapNotNull { forecastWarningAreaDto: ForecastWarningAreaDto -> toAuroraFeedEntity(forecastWarningAreaDto = forecastWarningAreaDto) }
        else -> {
            logger.warn { "Skipping Vadret API warning $id with unknown event $event" }
            emptyList()
        }
    }

private fun ForecastWarningDto.toAuroraFeedEntity(forecastWarningAreaDto: ForecastWarningAreaDto): AggregatedFeedEntity? {
    val publishedAt: Instant? = forecastWarningAreaDto.published.toInstantOrNull()
    val auroraChance: AuroraChance? = AuroraChance.entries.firstOrNull { auroraChance: AuroraChance -> auroraChance.name == forecastWarningAreaDto.level }
    val geohash: Geohash? = forecastWarningAreaDto.geohash.toGeohashOrNull()
    val score: Int? = forecastWarningAreaDto.score
    val auroraNight: String? = forecastWarningAreaDto.auroraNightOrNull(geohash = geohash)
    if (publishedAt == null || auroraChance == null || geohash == null || score == null || auroraNight == null) {
        logger.warn { "Skipping Vadret API aurora area ${forecastWarningAreaDto.id}, published ${forecastWarningAreaDto.published}, level ${forecastWarningAreaDto.level}, cell ${forecastWarningAreaDto.geohash}, score ${forecastWarningAreaDto.score}" }
        return null
    }
    return AggregatedFeedEntity(
        identifier = "vadret-$id-${forecastWarningAreaDto.id}-${auroraChance.name}",
        headline = "",
        details = emptyList(),
        source = FeedSource.VADRET_API.name,
        publishedAtEpochMillis = publishedAt.toEpochMilliseconds(),
        startsAtEpochMillis = forecastWarningAreaDto.approximateStart.toInstantOrNull()?.toEpochMilliseconds(),
        endsAtEpochMillis = forecastWarningAreaDto.approximateEnd.toInstantOrNull()?.toEpochMilliseconds(),
        polygons = forecastWarningAreaDto.area?.toGeoPolygonEntities().orEmpty(),
        lines = emptyList(),
        regions = emptyList(),
        aurora = AuroraEntity(
            chance = auroraChance.name,
            score = score,
            peakAtEpochMillis = forecastWarningAreaDto.peak.toInstantOrNull()?.toEpochMilliseconds(),
            geohash = geohash.value,
            auroraNight = auroraNight
        )
    )
}

private fun ForecastWarningAreaDto.auroraNightOrNull(geohash: Geohash?): String? {
    if (geohash == null) {
        return null
    }
    val geohashPrefix = "${geohash.value}$AREA_ID_SEPARATOR"
    return if (id.startsWith(prefix = geohashPrefix) && id.length > geohashPrefix.length) {
        id.removePrefix(prefix = geohashPrefix)
    } else {
        null
    }
}

private fun String?.toGeohashOrNull(): Geohash? =
    if (this == null) {
        null
    } else {
        geohashOrNull(value = this)
    }

private fun ForecastWarningDto.toAggregatedFeedEntity(forecastWarningAreaDto: ForecastWarningAreaDto): AggregatedFeedEntity? {
    val publishedAt: Instant? = forecastWarningAreaDto.published.toInstantOrNull()
    val slipperinessRisk: SlipperinessRisk? = forecastWarningAreaDto.level.toSlipperinessRiskOrNull()
    if (publishedAt == null || slipperinessRisk == null) {
        logger.warn { "Skipping Vadret API warning area ${forecastWarningAreaDto.id}, published ${forecastWarningAreaDto.published}, level ${forecastWarningAreaDto.level}" }
        return null
    }
    return AggregatedFeedEntity(
        identifier = "vadret-$id-${forecastWarningAreaDto.id}-${slipperinessRisk.name}",
        headline = "",
        details = emptyList(),
        source = FeedSource.VADRET_API.name,
        publishedAtEpochMillis = publishedAt.toEpochMilliseconds(),
        startsAtEpochMillis = forecastWarningAreaDto.approximateStart.toInstantOrNull()?.toEpochMilliseconds(),
        endsAtEpochMillis = forecastWarningAreaDto.approximateEnd.toInstantOrNull()?.toEpochMilliseconds(),
        polygons = forecastWarningAreaDto.area?.toGeoPolygonEntities().orEmpty(),
        lines = emptyList(),
        regions = emptyList(),
        slipperiness = SlipperinessEntity(
            risk = slipperinessRisk.name,
            causes = forecastWarningAreaDto.causes.toSlipperinessCauseNames()
        )
    )
}

private fun List<ForecastWarningCauseDto>?.toSlipperinessCauseNames(): List<String> =
    orEmpty()
        .mapNotNull { forecastWarningCauseDto: ForecastWarningCauseDto ->
            val slipperinessCause: SlipperinessCause? = SlipperinessCause.entries.firstOrNull { cause: SlipperinessCause -> cause.name == forecastWarningCauseDto.cause }
            val slipperinessRisk: SlipperinessRisk? = forecastWarningCauseDto.level.toSlipperinessRiskOrNull()
            if (slipperinessCause == null || slipperinessRisk == null) {
                logger.warn { "Skipping Vadret API slipperiness cause ${forecastWarningCauseDto.cause} at level ${forecastWarningCauseDto.level}" }
                null
            } else {
                slipperinessCause to slipperinessRisk
            }
        }.sortedWith(
            comparator = compareByDescending<Pair<SlipperinessCause, SlipperinessRisk>> { (_, slipperinessRisk: SlipperinessRisk) -> slipperinessRisk }
                .thenBy { (slipperinessCause: SlipperinessCause, _) -> slipperinessCause }
        ).map { (slipperinessCause: SlipperinessCause, _) -> slipperinessCause.name }

private fun String?.toSlipperinessRiskOrNull(): SlipperinessRisk? = SlipperinessRisk.entries.firstOrNull { slipperinessRisk: SlipperinessRisk -> slipperinessRisk.name == this }

private fun String?.toInstantOrNull(): Instant? =
    if (this == null) {
        null
    } else {
        Instant.parseOrNull(input = this)
    }