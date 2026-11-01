package se.vadretapp.vadret.aggregatedfeed.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AggregatedFeedCacheEntity(
    @SerialName(value = "fetched_at_epoch_millis")
    val fetchedAtEpochMillis: Long,
    @SerialName(value = "items")
    val items: List<AggregatedFeedEntity>
)