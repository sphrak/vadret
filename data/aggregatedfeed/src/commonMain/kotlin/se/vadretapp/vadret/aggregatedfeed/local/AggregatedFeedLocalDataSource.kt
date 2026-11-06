package se.vadretapp.vadret.aggregatedfeed.local

import kotlinx.coroutines.flow.Flow

internal interface AggregatedFeedLocalDataSource {

    suspend fun getAggregatedFeed(): AggregatedFeedCacheEntity?
    fun observeAggregatedFeed(): Flow<AggregatedFeedCacheEntity?>
    suspend fun setAggregatedFeed(aggregatedFeedCacheEntity: AggregatedFeedCacheEntity)
}