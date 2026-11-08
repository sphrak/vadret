package se.vadretapp.vadret.aggregatedfeed

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.aggregatedfeed.extension.toAggregatedFeedEntities
import se.vadretapp.vadret.aggregatedfeed.extension.toAggregatedFeedEntity
import se.vadretapp.vadret.aggregatedfeed.extension.toAggregatedFeeds
import se.vadretapp.vadret.aggregatedfeed.extension.toFailure
import se.vadretapp.vadret.aggregatedfeed.extension.toWarningFeed
import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedCacheEntity
import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedEntity
import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedLocalDataSource
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.ForecastWarningRequest
import se.vadretapp.vadret.aggregatedfeed.model.WarningFeed
import se.vadretapp.vadret.aggregatedfeed.remote.ForecastWarningRemoteDataSource
import se.vadretapp.vadret.aggregatedfeed.remote.KrisinformationFeedDto
import se.vadretapp.vadret.aggregatedfeed.remote.KrisinformationRemoteDataSource
import se.vadretapp.vadret.aggregatedfeed.remote.SmhiWarningDto
import se.vadretapp.vadret.aggregatedfeed.remote.SmhiWarningRemoteDataSource
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.aggregatedfeed")

internal class DefaultWarningsRepository(
    private val smhiWarningRemoteDataSource: SmhiWarningRemoteDataSource,
    private val krisinformationRemoteDataSource: KrisinformationRemoteDataSource,
    private val forecastWarningRemoteDataSource: ForecastWarningRemoteDataSource,
    private val aggregatedFeedLocalDataSource: AggregatedFeedLocalDataSource,
    private val clock: Clock
) : WarningsRepository {

    override suspend fun getAggregatedFeed(forecastWarningRequest: ForecastWarningRequest?): Either<Failure, List<AggregatedFeed>> =
        either {
            (aggregatedFeedLocalDataSource.getAggregatedFeed() ?: fetchAggregatedFeed(forecastWarningRequest = forecastWarningRequest)).toAggregatedFeeds()
        }.catch { exception: Exception -> exception.toFailure() }

    override fun observeAggregatedFeed(): Flow<Either<Failure, WarningFeed?>> =
        aggregatedFeedLocalDataSource
            .observeAggregatedFeed()
            .map<AggregatedFeedCacheEntity?, Either<Failure, WarningFeed?>> { aggregatedFeedCacheEntity: AggregatedFeedCacheEntity? ->
                val savedFeed: WarningFeed? = aggregatedFeedCacheEntity?.toWarningFeed()
                savedFeed.asRight()
            }.catch { throwable: Throwable ->
                if (throwable is Exception) {
                    emit(throwable.toFailure().asLeft())
                } else {
                    throw throwable
                }
            }

    override suspend fun refreshAggregatedFeed(forecastWarningRequest: ForecastWarningRequest?): Either<Failure, WarningFeed> =
        either { fetchAggregatedFeed(forecastWarningRequest = forecastWarningRequest).toWarningFeed() }
            .catch { exception: Exception -> exception.toFailure() }

    private suspend fun fetchAggregatedFeed(forecastWarningRequest: ForecastWarningRequest?): AggregatedFeedCacheEntity {
        val fetchedFeed = AggregatedFeedCacheEntity(
            fetchedAtEpochMillis = clock.now().toEpochMilliseconds(),
            items = fetchItems(forecastWarningRequest = forecastWarningRequest)
        )
        aggregatedFeedLocalDataSource.setAggregatedFeed(aggregatedFeedCacheEntity = fetchedFeed)
        return fetchedFeed
    }

    private suspend fun fetchItems(forecastWarningRequest: ForecastWarningRequest?): List<AggregatedFeedEntity> =
        coroutineScope {
            val smhiWarnings: Deferred<List<AggregatedFeedEntity>> = async { getSmhiWarnings() }
            val krisinformation: Deferred<List<AggregatedFeedEntity>> = async { getKrisinformation() }
            val forecastWarnings: Deferred<List<AggregatedFeedEntity>> = async { getForecastWarnings(forecastWarningRequest = forecastWarningRequest) }
            (smhiWarnings.await() + krisinformation.await() + forecastWarnings.await())
                .distinctBy { aggregatedFeedEntity: AggregatedFeedEntity -> aggregatedFeedEntity.identifier }
                .sortedByDescending { aggregatedFeedEntity: AggregatedFeedEntity -> aggregatedFeedEntity.publishedAtEpochMillis }
        }

    private suspend fun getSmhiWarnings(): List<AggregatedFeedEntity> =
        smhiWarningRemoteDataSource
            .getWarnings()
            .flatMap { smhiWarningDto: SmhiWarningDto -> smhiWarningDto.toAggregatedFeedEntities() }

    private suspend fun getKrisinformation(): List<AggregatedFeedEntity> {
        val news: List<KrisinformationFeedDto> = krisinformationRemoteDataSource.getNews()
        return getPublicAlerts().mapNotNull { krisinformationFeedDto: KrisinformationFeedDto -> krisinformationFeedDto.toAggregatedFeedEntity(isPublicAlert = true) } +
            news.mapNotNull { krisinformationFeedDto: KrisinformationFeedDto -> krisinformationFeedDto.toAggregatedFeedEntity(isPublicAlert = false) }
    }

    private suspend fun getForecastWarnings(forecastWarningRequest: ForecastWarningRequest?): List<AggregatedFeedEntity> {
        if (forecastWarningRequest == null || forecastWarningRequest.geohashes.isEmpty()) {
            return emptyList()
        }
        return try {
            forecastWarningRemoteDataSource
                .getWarnings(
                    installationToken = forecastWarningRequest.installationToken,
                    geohashes = forecastWarningRequest.geohashes
                ).toAggregatedFeedEntities()
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            logger.warn { "Keeping the last Vadret API warnings: ${exception::class.simpleName}: ${exception.message}" }
            savedForecastWarnings()
        }
    }

    private suspend fun savedForecastWarnings(): List<AggregatedFeedEntity> =
        aggregatedFeedLocalDataSource
            .getAggregatedFeed()
            ?.items
            .orEmpty()
            .filter { aggregatedFeedEntity: AggregatedFeedEntity -> aggregatedFeedEntity.source == FeedSource.VADRET_API.name }

    private suspend fun getPublicAlerts(): List<KrisinformationFeedDto> =
        try {
            krisinformationRemoteDataSource.getPublicAlerts()
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            logger.warn { "Skipping public alerts: ${exception::class.simpleName}: ${exception.message}" }
            emptyList()
        }
}