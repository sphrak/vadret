package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import se.vadretapp.vadret.common.DiskCache
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.weatherforecast.local.DefaultWeatherForecastLocalDataSource
import se.vadretapp.vadret.weatherforecast.model.Analysis
import se.vadretapp.vadret.weatherforecast.model.Forecast
import se.vadretapp.vadret.weatherforecast.remote.DefaultWeatherForecastRemoteDataSource

interface WeatherForecastRepository {

    suspend fun getForecasts(
        latitude: Double,
        longitude: Double
    ): Either<Failure, List<Forecast>>

    suspend fun getAnalyses(
        latitude: Double,
        longitude: Double
    ): Either<Failure, List<Analysis>>
}

fun WeatherForecastRepository(
    httpClient: Lazy<HttpClient>,
    diskCache: DiskCache,
    coroutineDispatcher: CoroutineDispatcher
): WeatherForecastRepository =
    DefaultWeatherForecastRepository(
        weatherForecastRemoteDataSource = DefaultWeatherForecastRemoteDataSource(httpClient = httpClient),
        weatherForecastLocalDataSource = DefaultWeatherForecastLocalDataSource(
            diskCache = diskCache,
            coroutineDispatcher = coroutineDispatcher
        )
    )