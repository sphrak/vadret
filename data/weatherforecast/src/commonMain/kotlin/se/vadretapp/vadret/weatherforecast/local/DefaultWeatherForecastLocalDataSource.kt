package se.vadretapp.vadret.weatherforecast.local

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import se.vadretapp.vadret.common.DiskCache
import se.vadretapp.vadret.common.defaultJson
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory

private const val FORECASTS_CACHE_KEY = "weather_forecasts.json"
private const val ANALYSES_CACHE_KEY = "weather_analyses.json"

// The followed places and the current location, with room for a few places the current location was at before.
private const val MAXIMUM_SAVED_PLACES = 10

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.weatherforecast")

private val savedForecastsSerializer: KSerializer<List<SavedForecastsEntity>> = ListSerializer(elementSerializer = SavedForecastsEntity.serializer())
private val savedAnalysesSerializer: KSerializer<List<SavedAnalysesEntity>> = ListSerializer(elementSerializer = SavedAnalysesEntity.serializer())

// The places last fetched are kept, newest first. A save reads, changes and writes the whole file, so saves take turns
// and no place saved at the same time is lost.
internal class DefaultWeatherForecastLocalDataSource(
    private val diskCache: DiskCache,
    private val coroutineDispatcher: CoroutineDispatcher
) : WeatherForecastLocalDataSource {

    private val mutex: Mutex = Mutex()

    override suspend fun getForecasts(placeKey: String): List<ForecastEntity>? =
        read(
            cacheKey = FORECASTS_CACHE_KEY,
            serializer = savedForecastsSerializer
        ).firstOrNull { savedForecastsEntity: SavedForecastsEntity -> savedForecastsEntity.placeKey == placeKey }
            ?.forecasts

    override suspend fun setForecasts(
        placeKey: String,
        forecastEntities: List<ForecastEntity>
    ) {
        update(
            cacheKey = FORECASTS_CACHE_KEY,
            serializer = savedForecastsSerializer
        ) { savedForecastsEntities: List<SavedForecastsEntity> ->
            listOf(
                SavedForecastsEntity(
                    placeKey = placeKey,
                    forecasts = forecastEntities
                )
            ) + savedForecastsEntities.filter { savedForecastsEntity: SavedForecastsEntity -> savedForecastsEntity.placeKey != placeKey }
        }
    }

    override suspend fun getAnalyses(placeKey: String): List<AnalysisEntity>? =
        read(
            cacheKey = ANALYSES_CACHE_KEY,
            serializer = savedAnalysesSerializer
        ).firstOrNull { savedAnalysesEntity: SavedAnalysesEntity -> savedAnalysesEntity.placeKey == placeKey }
            ?.analyses

    override suspend fun setAnalyses(
        placeKey: String,
        analysisEntities: List<AnalysisEntity>
    ) {
        update(
            cacheKey = ANALYSES_CACHE_KEY,
            serializer = savedAnalysesSerializer
        ) { savedAnalysesEntities: List<SavedAnalysesEntity> ->
            listOf(
                SavedAnalysesEntity(
                    placeKey = placeKey,
                    analyses = analysisEntities
                )
            ) + savedAnalysesEntities.filter { savedAnalysesEntity: SavedAnalysesEntity -> savedAnalysesEntity.placeKey != placeKey }
        }
    }

    private suspend fun <T> read(
        cacheKey: String,
        serializer: KSerializer<List<T>>
    ): List<T> =
        mutex.withLock {
            withContext(context = coroutineDispatcher) {
                decode(
                    cacheKey = cacheKey,
                    serializer = serializer
                )
            }
        }

    private suspend fun <T> update(
        cacheKey: String,
        serializer: KSerializer<List<T>>,
        change: (List<T>) -> List<T>
    ) {
        mutex.withLock {
            withContext(context = coroutineDispatcher) {
                val saved: List<T> = decode(
                    cacheKey = cacheKey,
                    serializer = serializer
                )
                diskCache.write(
                    key = cacheKey,
                    value = defaultJson.encodeToString(
                        serializer = serializer,
                        value = change(saved).take(n = MAXIMUM_SAVED_PLACES)
                    )
                )
            }
        }
    }

    private fun <T> decode(
        cacheKey: String,
        serializer: KSerializer<List<T>>
    ): List<T> {
        val text: String = diskCache.read(key = cacheKey) ?: return emptyList()
        return try {
            defaultJson.decodeFromString(
                deserializer = serializer,
                string = text
            )
        } catch (serializationException: SerializationException) {
            logger.warn { "Dropping unreadable saved weather $cacheKey: ${serializationException.message}" }
            diskCache.remove(key = cacheKey)
            emptyList()
        }
    }
}