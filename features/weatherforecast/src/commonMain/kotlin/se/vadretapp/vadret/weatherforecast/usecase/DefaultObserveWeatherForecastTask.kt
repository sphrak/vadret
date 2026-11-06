package se.vadretapp.vadret.weatherforecast.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.GetChosenPlaceTask
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.weatherforecast.WeatherForecastRepository
import se.vadretapp.vadret.weatherforecast.extension.toPlaceForecast
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast
import se.vadretapp.vadret.weatherforecast.model.RefreshedWeather
import se.vadretapp.vadret.weatherforecast.model.WeatherPlaceKind

internal class DefaultObserveWeatherForecastTask(
    private val getChosenPlaceTask: GetChosenPlaceTask,
    private val weatherForecastRepository: WeatherForecastRepository
) : ObserveWeatherForecastTask {

    override fun invoke(isRefresh: Boolean): Flow<Either<Failure, PlaceForecast>> =
        flow {
            when (val chosenPlace: Either<Failure, FollowedPlace> = getChosenPlaceTask()) {
                is Either.Left -> emit(value = chosenPlace.a.asLeft())
                is Either.Right -> emitAll(
                    flow = placeForecasts(
                        chosenPlace = chosenPlace.b,
                        isRefresh = isRefresh
                    )
                )
            }
        }

    private fun placeForecasts(
        chosenPlace: FollowedPlace,
        isRefresh: Boolean
    ): Flow<Either<Failure, PlaceForecast>> =
        weatherForecastRepository
            .observeRefreshedWeather(
                latitude = chosenPlace.latitude,
                longitude = chosenPlace.longitude,
                weatherPlaceKind = if (chosenPlace.favoriteId != null) {
                    WeatherPlaceKind.FAVORITE
                } else {
                    WeatherPlaceKind.CHOSEN_PLACE
                },
                isRefresh = isRefresh
            ).map { refreshedWeather: Either<Failure, RefreshedWeather> ->
                refreshedWeather.map { weather: RefreshedWeather ->
                    weather.toPlaceForecast(
                        placeName = chosenPlace.name,
                        municipality = chosenPlace.municipality,
                        county = chosenPlace.county,
                        latitude = chosenPlace.latitude,
                        longitude = chosenPlace.longitude
                    )
                }
            }
}