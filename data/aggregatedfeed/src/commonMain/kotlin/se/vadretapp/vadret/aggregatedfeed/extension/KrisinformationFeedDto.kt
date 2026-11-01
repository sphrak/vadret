package se.vadretapp.vadret.aggregatedfeed.extension

import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedEntity
import se.vadretapp.vadret.aggregatedfeed.local.FeedDetailEntity
import se.vadretapp.vadret.aggregatedfeed.local.FeedRegionEntity
import se.vadretapp.vadret.aggregatedfeed.model.FeedDetailKind
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegionKind
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.remote.KrisinformationAreaDto
import se.vadretapp.vadret.aggregatedfeed.remote.KrisinformationFeedDto
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import kotlin.time.Instant

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.aggregatedfeed")

private const val UNTITLED = ""

private val regionKindsByType: Map<String, FeedRegionKind> = mapOf(
    "Country" to FeedRegionKind.COUNTRY,
    "County" to FeedRegionKind.COUNTY,
    "Municipality" to FeedRegionKind.MUNICIPALITY
)

internal fun KrisinformationFeedDto.toAggregatedFeedEntity(isPublicAlert: Boolean): AggregatedFeedEntity? {
    val publishedAt: Instant? = Instant.parseOrNull(input = published)
    if (publishedAt == null) {
        logger.warn { "Skipping Krisinformation item $identifier, published $published" }
        return null
    }
    return AggregatedFeedEntity(
        identifier = "krisinformation-$identifier",
        headline = headline,
        details = listOfNotNull(
            preamble.toFeedDetailEntity(feedDetailKind = FeedDetailKind.SUMMARY),
            bodyText.toFeedDetailEntity(feedDetailKind = FeedDetailKind.BODY)
        ),
        source = FeedSource.KRISINFORMATION.name,
        isPublicAlert = isPublicAlert,
        senderName = senderName?.trim()?.ifEmpty { null },
        publishedAtEpochMillis = publishedAt.toEpochMilliseconds(),
        polygons = emptyList(),
        lines = emptyList(),
        regions = area
            .orEmpty()
            .mapNotNull { krisinformationAreaDto: KrisinformationAreaDto -> krisinformationAreaDto.toFeedRegionEntity() }
    )
}

private fun KrisinformationAreaDto.toFeedRegionEntity(): FeedRegionEntity? {
    val name: String = description?.trim()?.ifEmpty { null } ?: return null
    return FeedRegionEntity(
        kind = (regionKindsByType[type] ?: FeedRegionKind.OTHER).name,
        name = name
    )
}

private fun String?.toFeedDetailEntity(feedDetailKind: FeedDetailKind): FeedDetailEntity? =
    if (isNullOrBlank()) {
        null
    } else {
        FeedDetailEntity(
            kind = feedDetailKind.name,
            title = UNTITLED,
            text = trim()
        )
    }