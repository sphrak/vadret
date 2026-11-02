package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.extension.onResult
import io.github.sphrak.either.flatMap
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.nominatim.NominatimRepository
import se.vadretapp.vadret.nominatim.model.Locality
import se.vadretapp.vadret.weatherforecast.model.Analysis
import se.vadretapp.vadret.weatherforecast.model.Forecast
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast

internal class DefaultGetPlaceForecastTask(
    private val weatherForecastRepository: WeatherForecastRepository,
    private val nominatimRepository: NominatimRepository
) : GetPlaceForecastTask {

    override suspend fun invoke(
        latitude: Double,
        longitude: Double
    ): Either<Failure, PlaceForecast> =
        coroutineScope {
            val locality: Deferred<Locality?> = async {
                getLocality(
                    latitude = latitude,
                    longitude = longitude
                )
            }
            val analyses: Deferred<List<Analysis>> = async {
                getAnalyses(
                    latitude = latitude,
                    longitude = longitude
                )
            }
            val forecasts: Either<Failure, List<Forecast>> = weatherForecastRepository.getForecasts(
                latitude = latitude,
                longitude = longitude
            )
            if (forecasts.isLeft) {
                locality.cancel()
                analyses.cancel()
            }
            forecasts.flatMap { forecastList: List<Forecast> ->
                val foundLocality: Locality? = locality.await()
                PlaceForecast(
                    placeName = foundLocality?.name,
                    municipality = foundLocality?.municipality,
                    county = foundLocality?.county,
                    latitude = latitude,
                    longitude = longitude,
                    forecasts = forecastList,
                    analyses = analyses.await()
                ).asRight()
            }
        }

    private suspend fun getAnalyses(
        latitude: Double,
        longitude: Double
    ): List<Analysis> =
        weatherForecastRepository
            .getAnalyses(
                latitude = latitude,
                longitude = longitude
            ).onResult(
                onError = { _: Failure -> emptyList() },
                onSuccess = { analyses: List<Analysis> -> analyses }
            )

    private suspend fun getLocality(
        latitude: Double,
        longitude: Double
    ): Locality? =
        nominatimRepository
            .getLocality(
                latitude = latitude,
                longitude = longitude
            ).onResult(
                onError = { _: Failure -> null },
                onSuccess = { locality: Locality -> locality }
            )
}