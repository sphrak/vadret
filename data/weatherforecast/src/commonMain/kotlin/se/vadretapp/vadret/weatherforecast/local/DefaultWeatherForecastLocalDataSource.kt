package se.vadretapp.vadret.weatherforecast.local

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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

private const val WEATHER_CACHE_KEY = "weather.json"
private const val MAXIMUM_SAVED_PLACES = 10

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.weatherforecast")

private val savedWeatherSerializer: KSerializer<List<SavedWeatherEntity>> = ListSerializer(elementSerializer = SavedWeatherEntity.serializer())

internal class DefaultWeatherForecastLocalDataSource(
    private val diskCache: DiskCache,
    private val coroutineDispatcher: CoroutineDispatcher
) : WeatherForecastLocalDataSource {

    private val writeMutex: Mutex = Mutex()

    override fun observeWeather(placeKey: String): Flow<SavedWeatherEntity?> =
        diskCache
            .observe(key = WEATHER_CACHE_KEY)
            .map { text: String? ->
                decode(text = text).firstOrNull { savedWeatherEntity: SavedWeatherEntity -> savedWeatherEntity.placeKey == placeKey }
            }.distinctUntilChanged()
            .flowOn(context = coroutineDispatcher)

    override suspend fun setWeather(savedWeatherEntity: SavedWeatherEntity) {
        writeMutex.withLock {
            withContext(context = coroutineDispatcher) {
                val otherPlaces: List<SavedWeatherEntity> = decode(text = diskCache.read(key = WEATHER_CACHE_KEY))
                    .filter { savedWeather: SavedWeatherEntity -> savedWeather.placeKey != savedWeatherEntity.placeKey }
                diskCache.write(
                    key = WEATHER_CACHE_KEY,
                    value = defaultJson.encodeToString(
                        serializer = savedWeatherSerializer,
                        value = (listOf(savedWeatherEntity) + otherPlaces).take(n = MAXIMUM_SAVED_PLACES)
                    )
                )
            }
        }
    }

    private fun decode(text: String?): List<SavedWeatherEntity> {
        if (text == null) {
            return emptyList()
        }
        return try {
            defaultJson.decodeFromString(
                deserializer = savedWeatherSerializer,
                string = text
            )
        } catch (serializationException: SerializationException) {
            logger.warn { "Dropping unreadable saved weather: ${serializationException.message}" }
            diskCache.remove(key = WEATHER_CACHE_KEY)
            emptyList()
        }
    }
}