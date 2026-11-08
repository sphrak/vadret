package se.vadretapp.vadret.aggregatedfeed

import androidx.room3.Room
import androidx.sqlite.driver.NativeSQLiteDriver
import kotlinx.coroutines.CoroutineDispatcher
import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedDatabase
import kotlin.time.Clock

fun SeenWarningRepository(
    databaseDirectory: String,
    clock: Clock,
    coroutineDispatcher: CoroutineDispatcher
): SeenWarningRepository =
    SeenWarningRepository(
        databaseBuilder = Room
            .databaseBuilder<AggregatedFeedDatabase>(name = "$databaseDirectory/$AGGREGATED_FEED_DATABASE_NAME")
            .setDriver(driver = NativeSQLiteDriver())
            .setQueryCoroutineContext(context = coroutineDispatcher),
        clock = clock
    )