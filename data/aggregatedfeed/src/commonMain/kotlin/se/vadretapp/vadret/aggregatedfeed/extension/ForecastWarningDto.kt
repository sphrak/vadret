package se.vadretapp.vadret.aggregatedfeed.extension

import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedEntity
import se.vadretapp.vadret.aggregatedfeed.local.SlipperinessEntity
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.SlipperinessCause
import se.vadretapp.vadret.aggregatedfeed.model.SlipperinessRisk
import se.vadretapp.vadret.aggregatedfeed.remote.ForecastWarningAreaDto
import se.vadretapp.vadret.aggregatedfeed.remote.ForecastWarningCauseDto
import se.vadretapp.vadret.aggregatedfeed.remote.ForecastWarningDto
import se.vadretapp.vadret.aggregatedfeed.remote.ForecastWarningsDto
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import kotlin.time.Instant

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.aggregatedfeed")

private const val SLIPPERINESS_EVENT = "SLIPPERINESS"

internal fun ForecastWarningsDto.toAggregatedFeedEntities(): List<AggregatedFeedEntity> =
    warnings
        .orEmpty()
        .flatMap { forecastWarningDto: ForecastWarningDto -> forecastWarningDto.toAggregatedFeedEntities() }

private fun ForecastWarningDto.toAggregatedFeedEntities(): List<AggregatedFeedEntity> {
    if (event != SLIPPERINESS_EVENT) {
        logger.warn { "Skipping Vadret API warning $id with unknown event $event" }
        return emptyList()
    }
    return warningAreas
        .orEmpty()
        .mapNotNull { forecastWarningAreaDto: ForecastWarningAreaDto -> toAggregatedFeedEntity(forecastWarningAreaDto = forecastWarningAreaDto) }
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