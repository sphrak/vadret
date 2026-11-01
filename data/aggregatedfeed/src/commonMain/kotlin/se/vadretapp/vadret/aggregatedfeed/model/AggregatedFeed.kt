package se.vadretapp.vadret.aggregatedfeed.model

import kotlin.time.Instant

data class AggregatedFeed(
    val identifier: String,
    val headline: String,
    val areaName: String?,
    val warningLevel: WarningLevel?,
    val details: List<FeedDetail>,
    val source: FeedSource,
    val isPublicAlert: Boolean,
    val senderName: String?,
    val publishedAt: Instant,
    val startsAt: Instant?,
    val endsAt: Instant?,
    val area: FeedArea
)