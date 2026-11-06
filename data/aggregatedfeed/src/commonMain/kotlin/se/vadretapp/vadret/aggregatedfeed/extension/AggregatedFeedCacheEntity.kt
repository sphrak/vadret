package se.vadretapp.vadret.aggregatedfeed.extension

import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedCacheEntity
import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedEntity
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.SavedFeed
import kotlin.time.Instant

internal fun AggregatedFeedCacheEntity.toAggregatedFeeds(): List<AggregatedFeed> =
    items.mapNotNull { aggregatedFeedEntity: AggregatedFeedEntity -> aggregatedFeedEntity.toAggregatedFeed() }

internal fun AggregatedFeedCacheEntity?.toSavedFeed(): SavedFeed =
    SavedFeed(
        aggregatedFeeds = this?.toAggregatedFeeds().orEmpty(),
        fetchedAt = if (this != null) {
            Instant.fromEpochMilliseconds(epochMilliseconds = fetchedAtEpochMillis)
        } else {
            null
        }
    )