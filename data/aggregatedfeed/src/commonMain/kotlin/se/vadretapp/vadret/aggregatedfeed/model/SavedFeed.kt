package se.vadretapp.vadret.aggregatedfeed.model

import kotlin.time.Instant

data class SavedFeed(
    val aggregatedFeeds: List<AggregatedFeed>,
    val fetchedAt: Instant?
)