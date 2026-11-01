package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.weatherforecast.extension.toAnalysis
import se.vadretapp.vadret.weatherforecast.extension.toFailure
import se.vadretapp.vadret.weatherforecast.extension.toForecast
import se.vadretapp.vadret.weatherforecast.model.Analysis
import se.vadretapp.vadret.weatherforecast.model.Forecast
import se.vadretapp.vadret.weatherforecast.remote.AnalysisTimeSeriesDto
import se.vadretapp.vadret.weatherforecast.remote.TimeSeriesDto
import se.vadretapp.vadret.weatherforecast.remote.WeatherForecastRemoteDataSource

internal class DefaultWeatherForecastRepository(
    private val weatherForecastRemoteDataSource: WeatherForecastRemoteDataSource
) : WeatherForecastRepository {

    override suspend fun getForecasts(
        latitude: Double,
        longitude: Double
    ): Either<Failure, List<Forecast>> =
        either {
            weatherForecastRemoteDataSource
                .getWeatherForecast(
                    latitude = latitude,
                    longitude = longitude
                )
                .timeSeries
                .map { timeSeriesDto: TimeSeriesDto -> timeSeriesDto.toForecast() }
                .sortedBy { forecast: Forecast -> forecast.validAt }
        }.catch { exception: Exception -> exception.toFailure(notFoundErrorCode = ErrorCode.WEATHER_FORECAST_NOT_AVAILABLE_FOR_LOCATION) }

    override suspend fun getAnalyses(
        latitude: Double,
        longitude: Double
    ): Either<Failure, List<Analysis>> =
        either {
            weatherForecastRemoteDataSource
                .getAnalysis(
                    latitude = latitude,
                    longitude = longitude
                )
                .timeSeries
                .map { analysisTimeSeriesDto: AnalysisTimeSeriesDto -> analysisTimeSeriesDto.toAnalysis() }
                .sortedBy { analysis: Analysis -> analysis.analysedAt }
        }.catch { exception: Exception -> exception.toFailure(notFoundErrorCode = ErrorCode.WEATHER_ANALYSIS_NOT_AVAILABLE_FOR_LOCATION) }
}