package se.vadretapp.vadret.aggregatedfeed.model

import kotlin.time.Instant

data class WarningFeed(
    val aggregatedFeeds: List<AggregatedFeed>,
    val fetchedAt: Instant
)