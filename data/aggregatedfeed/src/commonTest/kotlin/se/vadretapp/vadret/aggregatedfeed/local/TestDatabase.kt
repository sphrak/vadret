package se.vadretapp.vadret.aggregatedfeed.local

import androidx.room3.Room
import kotlinx.coroutines.CoroutineDispatcher

internal fun inMemoryAggregatedFeedDatabase(coroutineDispatcher: CoroutineDispatcher): AggregatedFeedDatabase =
    Room
        .inMemoryDatabaseBuilder<AggregatedFeedDatabase>()
        .setQueryCoroutineContext(context = coroutineDispatcher)
        .forTests()
        .build()