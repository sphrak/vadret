package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.weatherforecast.extension.placeKey
import se.vadretapp.vadret.weatherforecast.extension.toAnalysis
import se.vadretapp.vadret.weatherforecast.extension.toAnalysisEntity
import se.vadretapp.vadret.weatherforecast.extension.toFailure
import se.vadretapp.vadret.weatherforecast.extension.toForecast
import se.vadretapp.vadret.weatherforecast.extension.toForecastEntity
import se.vadretapp.vadret.weatherforecast.local.AnalysisEntity
import se.vadretapp.vadret.weatherforecast.local.ForecastEntity
import se.vadretapp.vadret.weatherforecast.local.WeatherForecastLocalDataSource
import se.vadretapp.vadret.weatherforecast.model.Analysis
import se.vadretapp.vadret.weatherforecast.model.Forecast
import se.vadretapp.vadret.weatherforecast.remote.AnalysisTimeSeriesDto
import se.vadretapp.vadret.weatherforecast.remote.TimeSeriesDto
import se.vadretapp.vadret.weatherforecast.remote.WeatherForecastRemoteDataSource
import kotlin.coroutines.cancellation.CancellationException

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.weatherforecast")

// The forecast and the analysis are asked for every time and saved, so the last saved ones for the place are used when
// asking fails.
internal class DefaultWeatherForecastRepository(
    private val weatherForecastRemoteDataSource: WeatherForecastRemoteDataSource,
    private val weatherForecastLocalDataSource: WeatherForecastLocalDataSource
) : WeatherForecastRepository {

    override suspend fun getForecasts(
        latitude: Double,
        longitude: Double
    ): Either<Failure, List<Forecast>> =
        either {
            getForecastEntities(
                latitude = latitude,
                longitude = longitude
            ).map { forecastEntity: ForecastEntity -> forecastEntity.toForecast() }
        }.catch { exception: Exception -> exception.toFailure(notFoundErrorCode = ErrorCode.WEATHER_FORECAST_NOT_AVAILABLE_FOR_LOCATION) }

    override suspend fun getAnalyses(
        latitude: Double,
        longitude: Double
    ): Either<Failure, List<Analysis>> =
        either {
            getAnalysisEntities(
                latitude = latitude,
                longitude = longitude
            ).map { analysisEntity: AnalysisEntity -> analysisEntity.toAnalysis() }
        }.catch { exception: Exception -> exception.toFailure(notFoundErrorCode = ErrorCode.WEATHER_ANALYSIS_NOT_AVAILABLE_FOR_LOCATION) }

    private suspend fun getForecastEntities(
        latitude: Double,
        longitude: Double
    ): List<ForecastEntity> {
        val placeKey: String = placeKey(
            latitude = latitude,
            longitude = longitude
        )
        val fetchedForecasts: List<ForecastEntity> = try {
            weatherForecastRemoteDataSource
                .getWeatherForecast(
                    latitude = latitude,
                    longitude = longitude
                ).timeSeries
                .map { timeSeriesDto: TimeSeriesDto -> timeSeriesDto.toForecastEntity() }
                .sortedBy { forecastEntity: ForecastEntity -> forecastEntity.validAtEpochMillis }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            val savedForecasts: List<ForecastEntity> = weatherForecastLocalDataSource.getForecasts(placeKey = placeKey) ?: throw exception
            logger.warn { "Using the saved forecast for $placeKey: ${exception::class.simpleName}: ${exception.message}" }
            return savedForecasts
        }
        weatherForecastLocalDataSource.setForecasts(
            placeKey = placeKey,
            forecastEntities = fetchedForecasts
        )
        return fetchedForecasts
    }

    private suspend fun getAnalysisEntities(
        latitude: Double,
        longitude: Double
    ): List<AnalysisEntity> {
        val placeKey: String = placeKey(
            latitude = latitude,
            longitude = longitude
        )
        val fetchedAnalyses: List<AnalysisEntity> = try {
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
            val savedAnalyses: List<AnalysisEntity> = weatherForecastLocalDataSource.getAnalyses(placeKey = placeKey) ?: throw exception
            logger.warn { "Using the saved analysis for $placeKey: ${exception::class.simpleName}: ${exception.message}" }
            return savedAnalyses
        }
        weatherForecastLocalDataSource.setAnalyses(
            placeKey = placeKey,
            analysisEntities = fetchedAnalyses
        )
        return fetchedAnalyses
    }
}