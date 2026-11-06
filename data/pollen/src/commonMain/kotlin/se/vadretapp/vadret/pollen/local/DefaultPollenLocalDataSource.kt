package se.vadretapp.vadret.pollen.local

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

private const val POLLEN_TYPES_CACHE_KEY = "pollen_types.json"

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.pollen")

internal class DefaultPollenLocalDataSource(
    private val diskCache: DiskCache,
    private val coroutineDispatcher: CoroutineDispatcher
) : PollenLocalDataSource {

    override suspend fun getPollenTypes(): PollenTypesCacheEntity? =
        withContext(context = coroutineDispatcher) {
            decode(text = diskCache.read(key = POLLEN_TYPES_CACHE_KEY))
        }

    override fun observePollenTypes(): Flow<PollenTypesCacheEntity?> =
        diskCache
            .observe(key = POLLEN_TYPES_CACHE_KEY)
            .map { text: String? -> decode(text = text) }
            .flowOn(context = coroutineDispatcher)

    override suspend fun setPollenTypes(pollenTypesCacheEntity: PollenTypesCacheEntity) {
        withContext(context = coroutineDispatcher) {
            diskCache.write(
                key = POLLEN_TYPES_CACHE_KEY,
                value = defaultJson.encodeToString(value = pollenTypesCacheEntity)
            )
        }
    }

    private fun decode(text: String?): PollenTypesCacheEntity? {
        if (text == null) {
            return null
        }
        return try {
            defaultJson.decodeFromString<PollenTypesCacheEntity>(string = text)
        } catch (serializationException: SerializationException) {
            logger.warn { "Unreadable saved pollen types are left to be replaced: ${serializationException.message}" }
            null
        }
    }
}