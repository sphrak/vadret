package se.vadretapp.vadret.radar.local

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

private const val CACHE_KEY = "radar_geotiff.json"

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.radar")

internal class DefaultRadarLocalDataSource(
    private val diskCache: DiskCache,
    private val coroutineDispatcher: CoroutineDispatcher
) : RadarLocalDataSource {

    override suspend fun getRadar(): RadarCacheEntity? =
        withContext(context = coroutineDispatcher) {
            val text: String? = diskCache.read(key = CACHE_KEY)
            if (text != null) {
                decode(text = text)
            } else {
                null
            }
        }

    override fun observeRadar(): Flow<RadarCacheEntity?> =
        diskCache
            .observe(key = CACHE_KEY)
            .map { text: String? ->
                if (text != null) {
                    decode(text = text)
                } else {
                    null
                }
            }.flowOn(context = coroutineDispatcher)

    override suspend fun setRadar(radarCacheEntity: RadarCacheEntity) {
        withContext(context = coroutineDispatcher) {
            diskCache.write(
                key = CACHE_KEY,
                value = defaultJson.encodeToString(value = radarCacheEntity)
            )
        }
    }

    private fun decode(text: String): RadarCacheEntity? =
        try {
            defaultJson.decodeFromString<RadarCacheEntity>(string = text)
        } catch (serializationException: SerializationException) {
            logger.warn { "Unreadable radar saved data is left to be replaced: ${serializationException.message}" }
            null
        }
}