package se.vadretapp.vadret.aggregatedfeed

import androidx.room3.Room
import androidx.sqlite.driver.NativeSQLiteDriver
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedDatabase
import se.vadretapp.vadret.common.DiskCache
import kotlin.time.Clock

fun FeedRepository(
    databaseDirectory: String,
    httpClient: Lazy<HttpClient>,
    diskCache: DiskCache,
    clock: Clock,
    coroutineDispatcher: CoroutineDispatcher
): FeedRepository =
    FeedRepository(
        httpClient = httpClient,
        diskCache = diskCache,
        databaseBuilder = Room
            .databaseBuilder<AggregatedFeedDatabase>(name = "$databaseDirectory/$AGGREGATED_FEED_DATABASE_NAME")
            .setDriver(driver = NativeSQLiteDriver())
            .setQueryCoroutineContext(context = coroutineDispatcher),
        clock = clock,
        coroutineDispatcher = coroutineDispatcher
    )