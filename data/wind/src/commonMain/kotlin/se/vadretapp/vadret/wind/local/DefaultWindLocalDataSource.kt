package se.vadretapp.vadret.wind.local

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import se.vadretapp.vadret.common.DiskCache
import se.vadretapp.vadret.common.defaultJson
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory

private const val WIND_CACHE_KEY = "wind.json"

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.wind")

private val windJson: Json = Json(from = defaultJson) { allowSpecialFloatingPointValues = true }

internal class DefaultWindLocalDataSource(
    private val diskCache: DiskCache,
    private val coroutineDispatcher: CoroutineDispatcher
) : WindLocalDataSource {

    override suspend fun getWindForecast(): WindForecastEntity? =
        withContext(context = coroutineDispatcher) {
            decode(text = diskCache.read(key = WIND_CACHE_KEY))
        }

    override fun observeWindForecast(): Flow<WindForecastEntity?> =
        diskCache
            .observe(key = WIND_CACHE_KEY)
            .map { text: String? -> decode(text = text) }
            .flowOn(context = coroutineDispatcher)

    override suspend fun setWindForecast(windForecastEntity: WindForecastEntity) {
        withContext(context = coroutineDispatcher) {
            diskCache.write(
                key = WIND_CACHE_KEY,
                value = windJson.encodeToString(value = windForecastEntity)
            )
        }
    }

    private fun decode(text: String?): WindForecastEntity? {
        if (text == null) {
            return null
        }
        return try {
            windJson.decodeFromString<WindForecastEntity>(string = text)
        } catch (serializationException: SerializationException) {
            logger.warn { "Dropping unreadable saved wind: ${serializationException.message}" }
            diskCache.remove(key = WIND_CACHE_KEY)
            null
        }
    }
}