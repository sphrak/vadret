package se.vadretapp.vadret.aggregatedfeed.extension

import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedCacheEntity
import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedEntity
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.WarningFeed
import kotlin.time.Instant

internal fun AggregatedFeedCacheEntity.toAggregatedFeeds(): List<AggregatedFeed> =
    items.mapNotNull { aggregatedFeedEntity: AggregatedFeedEntity -> aggregatedFeedEntity.toAggregatedFeed() }

internal fun AggregatedFeedCacheEntity.toWarningFeed(): WarningFeed =
    WarningFeed(
        aggregatedFeeds = toAggregatedFeeds(),
        fetchedAt = Instant.fromEpochMilliseconds(epochMilliseconds = fetchedAtEpochMillis)
    )