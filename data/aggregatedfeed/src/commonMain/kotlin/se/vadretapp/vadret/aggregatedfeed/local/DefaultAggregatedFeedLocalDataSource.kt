package se.vadretapp.vadret.aggregatedfeed.local

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import se.vadretapp.vadret.common.DiskCache
import se.vadretapp.vadret.common.defaultJson
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory

private const val CACHE_KEY = "aggregatedfeed.json"

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.aggregatedfeed")

internal class DefaultAggregatedFeedLocalDataSource(
    private val diskCache: DiskCache,
    private val coroutineDispatcher: CoroutineDispatcher
) : AggregatedFeedLocalDataSource {

    override suspend fun getAggregatedFeed(): AggregatedFeedCacheEntity? =
        withContext(context = coroutineDispatcher) {
            val text: String? = diskCache.read(key = CACHE_KEY)
            if (text != null) {
                decode(text = text)
            } else {
                null
            }
        }

    override fun observeAggregatedFeed(): Flow<AggregatedFeedCacheEntity?> =
        diskCache
            .observe(key = CACHE_KEY)
            .map { text: String? ->
                if (text != null) {
                    decode(text = text)
                } else {
                    null
                }
            }.flowOn(context = coroutineDispatcher)

    override suspend fun setAggregatedFeed(aggregatedFeedCacheEntity: AggregatedFeedCacheEntity) {
        withContext(context = coroutineDispatcher) {
            diskCache.write(
                key = CACHE_KEY,
                value = defaultJson.encodeToString(value = aggregatedFeedCacheEntity)
            )
        }
    }

    private fun decode(text: String): AggregatedFeedCacheEntity? =
        try {
            defaultJson.decodeFromString<AggregatedFeedCacheEntity>(string = text)
        } catch (serializationException: SerializationException) {
            logger.warn { "Dropping unreadable feed cache: ${serializationException.message}" }
            diskCache.remove(key = CACHE_KEY)
            null
        }
}