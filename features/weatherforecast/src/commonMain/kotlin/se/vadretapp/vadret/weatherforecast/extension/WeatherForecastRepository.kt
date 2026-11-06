package se.vadretapp.vadret.weatherforecast.extension

import io.github.sphrak.either.Either
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.extension.whileRefreshing
import se.vadretapp.vadret.weatherforecast.WeatherForecastRepository
import se.vadretapp.vadret.weatherforecast.model.PlaceWeather
import se.vadretapp.vadret.weatherforecast.model.RefreshedWeather

internal fun WeatherForecastRepository.observeRefreshedWeather(
    latitude: Double,
    longitude: Double,
    isRefresh: Boolean
): Flow<Either<Failure, RefreshedWeather>> =
    observeWeather(
        latitude = latitude,
        longitude = longitude
    ).map { savedWeather: Either<Failure, PlaceWeather> -> savedWeather.toRefreshedWeather() }
        .whileRefreshing(
            refresh = {
                refreshWeather(
                    latitude = latitude,
                    longitude = longitude
                ).toRefreshedWeather()
            },
            isNothingSaved = { refreshedWeather: RefreshedWeather -> refreshedWeather.placeWeather.forecasts.isEmpty() },
            whenRefreshFails = { refreshedWeather: RefreshedWeather -> refreshedWeather.copy(isRefreshFailed = true) },
            isRefreshFirst = isRefresh
        )

private fun Either<Failure, PlaceWeather>.toRefreshedWeather(): Either<Failure, RefreshedWeather> =
    map { placeWeather: PlaceWeather ->
        RefreshedWeather(
            placeWeather = placeWeather,
            isRefreshFailed = false
        )
    }