package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.weatherforecast.extension.placeKey
import se.vadretapp.vadret.weatherforecast.extension.toAnalysis
import se.vadretapp.vadret.weatherforecast.extension.toAnalysisEntity
import se.vadretapp.vadret.weatherforecast.extension.toFailure
import se.vadretapp.vadret.weatherforecast.extension.toForecast
import se.vadretapp.vadret.weatherforecast.extension.toForecastEntity
import se.vadretapp.vadret.weatherforecast.extension.toPlaceWeather
import se.vadretapp.vadret.weatherforecast.local.AnalysisEntity
import se.vadretapp.vadret.weatherforecast.local.ForecastEntity
import se.vadretapp.vadret.weatherforecast.local.SavedWeatherEntity
import se.vadretapp.vadret.weatherforecast.local.WeatherForecastLocalDataSource
import se.vadretapp.vadret.weatherforecast.model.Analysis
import se.vadretapp.vadret.weatherforecast.model.Forecasts
import se.vadretapp.vadret.weatherforecast.model.PlaceWeather
import se.vadretapp.vadret.weatherforecast.model.WeatherPlaceKind
import se.vadretapp.vadret.weatherforecast.remote.AnalysisTimeSeriesDto
import se.vadretapp.vadret.weatherforecast.remote.TimeSeriesDto
import se.vadretapp.vadret.weatherforecast.remote.WeatherForecastRemoteDataSource
import kotlin.time.Clock
import kotlin.time.Instant

internal class DefaultWeatherForecastRepository(
    private val weatherForecastRemoteDataSource: WeatherForecastRemoteDataSource,
    private val weatherForecastLocalDataSource: WeatherForecastLocalDataSource,
    private val clock: Clock
) : WeatherForecastRepository {

    override fun observeWeather(
        latitude: Double,
        longitude: Double
    ): Flow<Either<Failure, PlaceWeather?>> =
        weatherForecastLocalDataSource
            .observeWeather(
                placeKey = placeKey(
                    latitude = latitude,
                    longitude = longitude
                )
            ).map<SavedWeatherEntity?, Either<Failure, PlaceWeather?>> { savedWeatherEntity: SavedWeatherEntity? ->
                val savedPlaceWeather: PlaceWeather? = savedWeatherEntity?.toPlaceWeather()
                savedPlaceWeather.asRight()
            }.catch { throwable: Throwable ->
                if (throwable is Exception) {
                    emit(value = throwable.toFailure(notFoundErrorCode = ErrorCode.WEATHER_FORECAST_NOT_AVAILABLE_FOR_LOCATION).asLeft())
                } else {
                    throw throwable
                }
            }

    override suspend fun refreshForecasts(
        latitude: Double,
        longitude: Double,
        weatherPlaceKind: WeatherPlaceKind
    ): Either<Failure, Forecasts> =
        either {
            val fetchedAt: Instant = clock.now()
            val forecastEntities: List<ForecastEntity> = weatherForecastRemoteDataSource
                .getWeatherForecast(
                    latitude = latitude,
                    longitude = longitude
                ).timeSeries
                .map { timeSeriesDto: TimeSeriesDto -> timeSeriesDto.toForecastEntity() }
                .sortedBy { forecastEntity: ForecastEntity -> forecastEntity.validAtEpochMillis }
            if (weatherPlaceKind != WeatherPlaceKind.DROPPED_PIN) {
                weatherForecastLocalDataSource.setForecasts(
                    placeKey = placeKey(
                        latitude = latitude,
                        longitude = longitude
                    ),
                    isChosenPlace = weatherPlaceKind == WeatherPlaceKind.CHOSEN_PLACE,
                    forecastEntities = forecastEntities,
                    fetchedAtEpochMillis = fetchedAt.toEpochMilliseconds()
                )
            }
            Forecasts(
                forecasts = forecastEntities.map { forecastEntity: ForecastEntity -> forecastEntity.toForecast() },
                fetchedAt = fetchedAt
            )
        }.catch { exception: Exception -> exception.toFailure(notFoundErrorCode = ErrorCode.WEATHER_FORECAST_NOT_AVAILABLE_FOR_LOCATION) }

    override suspend fun refreshAnalyses(
        latitude: Double,
        longitude: Double,
        weatherPlaceKind: WeatherPlaceKind
    ): Either<Failure, List<Analysis>> =
        either {
            val analysisEntities: List<AnalysisEntity> = weatherForecastRemoteDataSource
                .getAnalysis(
                    latitude = latitude,
                    longitude = longitude
                ).timeSeries
                .map { analysisTimeSeriesDto: AnalysisTimeSeriesDto -> analysisTimeSeriesDto.toAnalysisEntity() }
                .sortedBy { analysisEntity: AnalysisEntity -> analysisEntity.analysedAtEpochMillis }
            if (weatherPlaceKind != WeatherPlaceKind.DROPPED_PIN) {
                weatherForecastLocalDataSource.setAnalyses(
                    placeKey = placeKey(
                        latitude = latitude,
                        longitude = longitude
                    ),
                    isChosenPlace = weatherPlaceKind == WeatherPlaceKind.CHOSEN_PLACE,
                    analysisEntities = analysisEntities
                )
            }
            analysisEntities.map { analysisEntity: AnalysisEntity -> analysisEntity.toAnalysis() }
        }.catch { exception: Exception -> exception.toFailure(notFoundErrorCode = ErrorCode.WEATHER_ANALYSIS_NOT_AVAILABLE_FOR_LOCATION) }
}