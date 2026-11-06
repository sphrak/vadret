package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.DiskCache
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.weatherforecast.local.DefaultWeatherForecastLocalDataSource
import se.vadretapp.vadret.weatherforecast.model.Analysis
import se.vadretapp.vadret.weatherforecast.model.Forecasts
import se.vadretapp.vadret.weatherforecast.model.PlaceWeather
import se.vadretapp.vadret.weatherforecast.model.WeatherPlaceKind
import se.vadretapp.vadret.weatherforecast.remote.DefaultWeatherForecastRemoteDataSource
import kotlin.time.Clock

interface WeatherForecastRepository {

    fun observeWeather(
        latitude: Double,
        longitude: Double
    ): Flow<Either<Failure, PlaceWeather?>>

    suspend fun refreshForecasts(
        latitude: Double,
        longitude: Double,
        weatherPlaceKind: WeatherPlaceKind
    ): Either<Failure, Forecasts>

    suspend fun refreshAnalyses(
        latitude: Double,
        longitude: Double,
        weatherPlaceKind: WeatherPlaceKind
    ): Either<Failure, List<Analysis>>
}

fun WeatherForecastRepository(
    httpClient: Lazy<HttpClient>,
    diskCache: DiskCache,
    clock: Clock,
    coroutineDispatcher: CoroutineDispatcher
): WeatherForecastRepository =
    DefaultWeatherForecastRepository(
        weatherForecastRemoteDataSource = DefaultWeatherForecastRemoteDataSource(httpClient = httpClient),
        weatherForecastLocalDataSource = DefaultWeatherForecastLocalDataSource(
            diskCache = diskCache,
            coroutineDispatcher = coroutineDispatcher
        ),
        clock = clock
    )