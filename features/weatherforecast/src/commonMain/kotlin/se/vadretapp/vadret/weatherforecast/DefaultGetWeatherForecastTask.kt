package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.extension.onResult
import io.github.sphrak.either.flatMap
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.GetChosenPlaceTask
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.weatherforecast.model.Analysis
import se.vadretapp.vadret.weatherforecast.model.Forecast
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast

internal class DefaultGetWeatherForecastTask(
    private val getChosenPlaceTask: GetChosenPlaceTask,
    private val weatherForecastRepository: WeatherForecastRepository
) : GetWeatherForecastTask {

    override suspend fun invoke(): Either<Failure, PlaceForecast> =
        getChosenPlaceTask().flatMap { chosenPlace: FollowedPlace -> getPlaceForecast(chosenPlace = chosenPlace) }

    private suspend fun getPlaceForecast(chosenPlace: FollowedPlace): Either<Failure, PlaceForecast> =
        coroutineScope {
            val analyses: Deferred<List<Analysis>> = async {
                getAnalyses(
                    latitude = chosenPlace.latitude,
                    longitude = chosenPlace.longitude
                )
            }
            val forecasts: Either<Failure, List<Forecast>> = weatherForecastRepository.getForecasts(
                latitude = chosenPlace.latitude,
                longitude = chosenPlace.longitude
            )
            if (forecasts.isLeft) {
                analyses.cancel()
            }
            forecasts.flatMap { forecastList: List<Forecast> ->
                PlaceForecast(
                    placeName = chosenPlace.name,
                    municipality = chosenPlace.municipality,
                    county = chosenPlace.county,
                    latitude = chosenPlace.latitude,
                    longitude = chosenPlace.longitude,
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
}