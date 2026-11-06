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

    override suspend fun getPollenTypes(): List<PollenTypeEntity>? =
        withContext(context = coroutineDispatcher) {
            val text: String? = diskCache.read(key = POLLEN_TYPES_CACHE_KEY)
            if (text != null) {
                decode(text = text)
            } else {
                null
            }
        }

    override fun observePollenTypes(): Flow<List<PollenTypeEntity>?> =
        diskCache
            .observe(key = POLLEN_TYPES_CACHE_KEY)
            .map { text: String? ->
                if (text != null) {
                    decode(text = text)
                } else {
                    null
                }
            }.flowOn(context = coroutineDispatcher)

    override suspend fun setPollenTypes(pollenTypeEntities: List<PollenTypeEntity>) {
        withContext(context = coroutineDispatcher) {
            diskCache.write(
                key = POLLEN_TYPES_CACHE_KEY,
                value = defaultJson.encodeToString(value = pollenTypeEntities)
            )
        }
    }

    private fun decode(text: String): List<PollenTypeEntity>? =
        try {
            defaultJson.decodeFromString<List<PollenTypeEntity>>(string = text)
        } catch (serializationException: SerializationException) {
            logger.warn { "Dropping unreadable saved pollen types: ${serializationException.message}" }
            diskCache.remove(key = POLLEN_TYPES_CACHE_KEY)
            null
        }
}