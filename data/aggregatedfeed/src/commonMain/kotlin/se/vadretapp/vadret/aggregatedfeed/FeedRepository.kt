package se.vadretapp.vadret.aggregatedfeed

import androidx.room3.RoomDatabase
import io.github.sphrak.either.Either
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedDatabase
import se.vadretapp.vadret.aggregatedfeed.local.DefaultAggregatedFeedLocalDataSource
import se.vadretapp.vadret.aggregatedfeed.local.DefaultSeenWarningLocalDataSource
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.ForecastWarningRequest
import se.vadretapp.vadret.aggregatedfeed.model.SeenWarning
import se.vadretapp.vadret.aggregatedfeed.model.WarningFeed
import se.vadretapp.vadret.aggregatedfeed.remote.DefaultForecastWarningRemoteDataSource
import se.vadretapp.vadret.aggregatedfeed.remote.DefaultKrisinformationRemoteDataSource
import se.vadretapp.vadret.aggregatedfeed.remote.DefaultSmhiWarningRemoteDataSource
import se.vadretapp.vadret.common.DiskCache
import se.vadretapp.vadret.common.Failure
import kotlin.time.Clock

internal const val AGGREGATED_FEED_DATABASE_NAME = "aggregatedfeed.db"

interface FeedRepository {

    suspend fun getAggregatedFeed(forecastWarningRequest: ForecastWarningRequest?): Either<Failure, List<AggregatedFeed>>

    fun observeAggregatedFeed(): Flow<Either<Failure, WarningFeed?>>

    suspend fun refreshAggregatedFeed(forecastWarningRequest: ForecastWarningRequest?): Either<Failure, WarningFeed>

    fun observeSeenWarnings(): Flow<Either<Failure, List<SeenWarning>>>

    suspend fun markSeen(warningIdentifier: String): Either<Failure, Unit>
}

internal fun FeedRepository(
    httpClient: Lazy<HttpClient>,
    diskCache: DiskCache,
    databaseBuilder: RoomDatabase.Builder<AggregatedFeedDatabase>,
    clock: Clock,
    coroutineDispatcher: CoroutineDispatcher
): FeedRepository =
    DefaultFeedRepository(
        smhiWarningRemoteDataSource = DefaultSmhiWarningRemoteDataSource(httpClient = httpClient),
        krisinformationRemoteDataSource = DefaultKrisinformationRemoteDataSource(httpClient = httpClient),
        forecastWarningRemoteDataSource = DefaultForecastWarningRemoteDataSource(httpClient = httpClient),
        aggregatedFeedLocalDataSource = DefaultAggregatedFeedLocalDataSource(
            diskCache = diskCache,
            coroutineDispatcher = coroutineDispatcher
        ),
        seenWarningLocalDataSource = DefaultSeenWarningLocalDataSource(aggregatedFeedDatabase = databaseBuilder.build()),
        clock = clock
    )