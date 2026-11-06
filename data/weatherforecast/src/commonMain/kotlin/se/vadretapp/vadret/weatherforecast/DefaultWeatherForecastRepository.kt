package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.weatherforecast.extension.placeKey
import se.vadretapp.vadret.weatherforecast.extension.toAnalysisEntity
import se.vadretapp.vadret.weatherforecast.extension.toFailure
import se.vadretapp.vadret.weatherforecast.extension.toForecastEntity
import se.vadretapp.vadret.weatherforecast.extension.toPlaceWeather
import se.vadretapp.vadret.weatherforecast.local.AnalysisEntity
import se.vadretapp.vadret.weatherforecast.local.ForecastEntity
import se.vadretapp.vadret.weatherforecast.local.SavedWeatherEntity
import se.vadretapp.vadret.weatherforecast.local.WeatherForecastLocalDataSource
import se.vadretapp.vadret.weatherforecast.model.PlaceWeather
import se.vadretapp.vadret.weatherforecast.remote.AnalysisTimeSeriesDto
import se.vadretapp.vadret.weatherforecast.remote.TimeSeriesDto
import se.vadretapp.vadret.weatherforecast.remote.WeatherForecastRemoteDataSource
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.weatherforecast")

internal class DefaultWeatherForecastRepository(
    private val weatherForecastRemoteDataSource: WeatherForecastRemoteDataSource,
    private val weatherForecastLocalDataSource: WeatherForecastLocalDataSource,
    private val clock: Clock
) : WeatherForecastRepository {

    override fun observeWeather(
        latitude: Double,
        longitude: Double
    ): Flow<Either<Failure, PlaceWeather>> =
        weatherForecastLocalDataSource
            .observeWeather(
                placeKey = placeKey(
                    latitude = latitude,
                    longitude = longitude
                )
            ).map<SavedWeatherEntity?, Either<Failure, PlaceWeather>> { savedWeatherEntity: SavedWeatherEntity? ->
                savedWeatherEntity.toPlaceWeather().asRight()
            }.catch { throwable: Throwable ->
                if (throwable is Exception) {
                    emit(value = throwable.toFailure(notFoundErrorCode = ErrorCode.WEATHER_FORECAST_NOT_AVAILABLE_FOR_LOCATION).asLeft())
                } else {
                    throw throwable
                }
            }

    override suspend fun refreshWeather(
        latitude: Double,
        longitude: Double
    ): Either<Failure, PlaceWeather> =
        either {
            fetchWeather(
                latitude = latitude,
                longitude = longitude
            ).toPlaceWeather()
        }.catch { exception: Exception -> exception.toFailure(notFoundErrorCode = ErrorCode.WEATHER_FORECAST_NOT_AVAILABLE_FOR_LOCATION) }

    private suspend fun fetchWeather(
        latitude: Double,
        longitude: Double
    ): SavedWeatherEntity =
        coroutineScope {
            val analyses: Deferred<List<AnalysisEntity>> = async {
                fetchAnalyses(
                    latitude = latitude,
                    longitude = longitude
                )
            }
            val forecasts: List<ForecastEntity> = weatherForecastRemoteDataSource
                .getWeatherForecast(
                    latitude = latitude,
                    longitude = longitude
                ).timeSeries
                .map { timeSeriesDto: TimeSeriesDto -> timeSeriesDto.toForecastEntity() }
                .sortedBy { forecastEntity: ForecastEntity -> forecastEntity.validAtEpochMillis }
            val savedWeatherEntity = SavedWeatherEntity(
                placeKey = placeKey(
                    latitude = latitude,
                    longitude = longitude
                ),
                fetchedAtEpochMillis = clock.now().toEpochMilliseconds(),
                forecasts = forecasts,
                analyses = analyses.await()
            )
            weatherForecastLocalDataSource.setWeather(savedWeatherEntity = savedWeatherEntity)
            savedWeatherEntity
        }

    private suspend fun fetchAnalyses(
        latitude: Double,
        longitude: Double
    ): List<AnalysisEntity> =
        try {
            weatherForecastRemoteDataSource
                .getAnalysis(
                    latitude = latitude,
                    longitude = longitude
                ).timeSeries
                .map { analysisTimeSeriesDto: AnalysisTimeSeriesDto -> analysisTimeSeriesDto.toAnalysisEntity() }
                .sortedBy { analysisEntity: AnalysisEntity -> analysisEntity.analysedAtEpochMillis }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            currentCoroutineContext().ensureActive()
            val analysisFailure: Failure = exception.toFailure(notFoundErrorCode = ErrorCode.WEATHER_ANALYSIS_NOT_AVAILABLE_FOR_LOCATION)
            logger.info { "${analysisFailure.errorCode}: the forecast is saved without the past hours" }
            emptyList()
        }
}