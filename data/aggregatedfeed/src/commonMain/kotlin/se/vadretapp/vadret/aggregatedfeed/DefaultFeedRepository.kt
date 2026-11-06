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
import se.vadretapp.vadret.aggregatedfeed.extension.toAggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.extension.toAggregatedFeedEntities
import se.vadretapp.vadret.aggregatedfeed.extension.toAggregatedFeedEntity
import se.vadretapp.vadret.aggregatedfeed.extension.toFailure
import se.vadretapp.vadret.aggregatedfeed.extension.toSeenWarning
import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedCacheEntity
import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedEntity
import se.vadretapp.vadret.aggregatedfeed.local.AggregatedFeedLocalDataSource
import se.vadretapp.vadret.aggregatedfeed.local.SeenWarningEntity
import se.vadretapp.vadret.aggregatedfeed.local.SeenWarningLocalDataSource
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.SeenWarning
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.aggregatedfeed")

private val cacheLifetime: Duration = 5.minutes

internal class DefaultFeedRepository(
    private val smhiWarningRemoteDataSource: SmhiWarningRemoteDataSource,
    private val krisinformationRemoteDataSource: KrisinformationRemoteDataSource,
    private val aggregatedFeedLocalDataSource: AggregatedFeedLocalDataSource,
    private val seenWarningLocalDataSource: SeenWarningLocalDataSource,
    private val clock: Clock
) : FeedRepository {

    override suspend fun getAggregatedFeed(isRefresh: Boolean): Either<Failure, List<AggregatedFeed>> =
        either {
            getAggregatedFeedCacheEntity(isRefresh = isRefresh)
                .items
                .mapNotNull { aggregatedFeedEntity: AggregatedFeedEntity -> aggregatedFeedEntity.toAggregatedFeed() }
        }.catch { exception: Exception -> exception.toFailure() }

    override fun observeSeenWarnings(): Flow<Either<Failure, List<SeenWarning>>> =
        seenWarningLocalDataSource
            .observeSeenWarnings()
            .map<List<SeenWarningEntity>, Either<Failure, List<SeenWarning>>> { seenWarningEntities: List<SeenWarningEntity> ->
                seenWarningEntities
                    .map { seenWarningEntity: SeenWarningEntity -> seenWarningEntity.toSeenWarning() }
                    .asRight()
            }.catch { throwable: Throwable ->
                if (throwable is Exception) {
                    emit(throwable.toFailure().asLeft())
                } else {
                    throw throwable
                }
            }

    override suspend fun markSeen(warningIdentifier: String): Either<Failure, Unit> =
        either {
            seenWarningLocalDataSource.markSeen(
                seenWarningEntity = SeenWarningEntity(
                    warningIdentifier = warningIdentifier,
                    seenAtEpochMillis = clock.now().toEpochMilliseconds()
                )
            )
        }.catch { exception: Exception -> exception.toFailure() }

    private suspend fun getAggregatedFeedCacheEntity(isRefresh: Boolean): AggregatedFeedCacheEntity {
        val now: Instant = clock.now()
        val cachedFeed: AggregatedFeedCacheEntity? = aggregatedFeedLocalDataSource.getAggregatedFeed()
        return if (!isRefresh && cachedFeed != null && cachedFeed.isFresh(now = now)) {
            cachedFeed
        } else {
            fetchAggregatedFeed(
                now = now,
                cachedFeed = cachedFeed
            )
        }
    }

    // A refresh or an old feed is fetched again, and the saved feed is used when fetching fails, however old it is.
    private suspend fun fetchAggregatedFeed(
        now: Instant,
        cachedFeed: AggregatedFeedCacheEntity?
    ): AggregatedFeedCacheEntity {
        val fetchedFeed: AggregatedFeedCacheEntity = try {
            AggregatedFeedCacheEntity(
                fetchedAtEpochMillis = now.toEpochMilliseconds(),
                items = fetchItems()
            )
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            if (cachedFeed == null) {
                throw exception
            }
            logger.warn { "Using the feed fetched at ${cachedFeed.fetchedAtEpochMillis}: ${exception::class.simpleName}: ${exception.message}" }
            return cachedFeed
        }
        aggregatedFeedLocalDataSource.setAggregatedFeed(aggregatedFeedCacheEntity = fetchedFeed)
        return fetchedFeed
    }

    private suspend fun fetchItems(): List<AggregatedFeedEntity> =
        coroutineScope {
            val smhiWarnings: Deferred<List<AggregatedFeedEntity>> = async { getSmhiWarnings() }
            val krisinformation: Deferred<List<AggregatedFeedEntity>> = async { getKrisinformation() }
            (smhiWarnings.await() + krisinformation.await())
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

    private suspend fun getPublicAlerts(): List<KrisinformationFeedDto> =
        try {
            krisinformationRemoteDataSource.getPublicAlerts()
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            logger.warn { "Skipping public alerts: ${exception::class.simpleName}: ${exception.message}" }
            emptyList()
        }

    private fun AggregatedFeedCacheEntity.isFresh(now: Instant): Boolean {
        val age: Duration = now - Instant.fromEpochMilliseconds(epochMilliseconds = fetchedAtEpochMillis)
        return age >= Duration.ZERO && age < cacheLifetime
    }
}