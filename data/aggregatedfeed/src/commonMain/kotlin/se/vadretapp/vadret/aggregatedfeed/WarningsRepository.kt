package se.vadretapp.vadret.aggregatedfeed

import io.github.sphrak.either.Either
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.aggregatedfeed.local.DefaultAggregatedFeedLocalDataSource
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.ForecastWarningRequest
import se.vadretapp.vadret.aggregatedfeed.model.WarningFeed
import se.vadretapp.vadret.aggregatedfeed.remote.DefaultForecastWarningRemoteDataSource
import se.vadretapp.vadret.aggregatedfeed.remote.DefaultKrisinformationRemoteDataSource
import se.vadretapp.vadret.aggregatedfeed.remote.DefaultSmhiWarningRemoteDataSource
import se.vadretapp.vadret.common.DiskCache
import se.vadretapp.vadret.common.Failure
import kotlin.time.Clock

interface WarningsRepository {

    suspend fun getAggregatedFeed(forecastWarningRequest: ForecastWarningRequest?): Either<Failure, List<AggregatedFeed>>

    fun observeAggregatedFeed(): Flow<Either<Failure, WarningFeed?>>

    suspend fun refreshAggregatedFeed(forecastWarningRequest: ForecastWarningRequest?): Either<Failure, WarningFeed>
}

fun WarningsRepository(
    httpClient: Lazy<HttpClient>,
    diskCache: DiskCache,
    clock: Clock,
    coroutineDispatcher: CoroutineDispatcher
): WarningsRepository =
    DefaultWarningsRepository(
        smhiWarningRemoteDataSource = DefaultSmhiWarningRemoteDataSource(httpClient = httpClient),
        krisinformationRemoteDataSource = DefaultKrisinformationRemoteDataSource(httpClient = httpClient),
        forecastWarningRemoteDataSource = DefaultForecastWarningRemoteDataSource(httpClient = httpClient),
        aggregatedFeedLocalDataSource = DefaultAggregatedFeedLocalDataSource(
            diskCache = diskCache,
            coroutineDispatcher = coroutineDispatcher
        ),
        clock = clock
    )