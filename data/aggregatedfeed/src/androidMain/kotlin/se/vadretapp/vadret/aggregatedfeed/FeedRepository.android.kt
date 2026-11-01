package se.vadretapp.vadret.aggregatedfeed

import android.content.Context
import androidx.room3.Room
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedDatabase
import se.vadretapp.vadret.common.DiskCache
import kotlin.time.Clock

fun FeedRepository(
    context: Context,
    httpClient: Lazy<HttpClient>,
    diskCache: DiskCache,
    clock: Clock,
    coroutineDispatcher: CoroutineDispatcher
): FeedRepository =
    FeedRepository(
        httpClient = httpClient,
        diskCache = diskCache,
        databaseBuilder = Room.databaseBuilder<AggregatedFeedDatabase>(
            context = context,
            name = AGGREGATED_FEED_DATABASE_NAME
        ),
        clock = clock,
        coroutineDispatcher = coroutineDispatcher
    )