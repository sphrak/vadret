package se.vadretapp.vadret.weatherforecast.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.extension.successOrNull
import io.github.sphrak.either.map
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.refresh.whileRefreshing
import se.vadretapp.vadret.weatherforecast.WeatherForecastRepository
import se.vadretapp.vadret.weatherforecast.extension.toRefreshedWeather
import se.vadretapp.vadret.weatherforecast.model.Analysis
import se.vadretapp.vadret.weatherforecast.model.Forecasts
import se.vadretapp.vadret.weatherforecast.model.PlaceWeather
import se.vadretapp.vadret.weatherforecast.model.RefreshedWeather
import se.vadretapp.vadret.weatherforecast.model.WeatherPlaceKind

internal fun WeatherForecastRepository.observeRefreshedWeather(
    latitude: Double,
    longitude: Double,
    weatherPlaceKind: WeatherPlaceKind,
    isRefresh: Boolean
): Flow<Either<Failure, RefreshedWeather>> =
    observeWeather(
        latitude = latitude,
        longitude = longitude
    ).map { savedWeather: Either<Failure, PlaceWeather?> ->
        savedWeather.map { saved: PlaceWeather? -> saved?.toRefreshedWeather() }
    }.whileRefreshing(
        refresh = {
            refreshPlaceWeather(
                latitude = latitude,
                longitude = longitude,
                weatherPlaceKind = weatherPlaceKind
            ).map { saved: PlaceWeather -> saved.toRefreshedWeather() }
        },
        isNothingSaved = { refreshedWeather: RefreshedWeather? -> refreshedWeather == null || refreshedWeather.placeWeather.forecasts.isEmpty() },
        whenRefreshFails = { refreshedWeather: RefreshedWeather? -> refreshedWeather?.copy(isRefreshFailed = true) },
        isRefreshFirst = isRefresh
    ).mapNotNull { refreshedWeather: Either<Failure, RefreshedWeather?> ->
        when (refreshedWeather) {
            is Either.Left -> refreshedWeather
            is Either.Right -> refreshedWeather.b?.asRight()
        }
    }

private suspend fun WeatherForecastRepository.refreshPlaceWeather(
    latitude: Double,
    longitude: Double,
    weatherPlaceKind: WeatherPlaceKind
): Either<Failure, PlaceWeather> =
    coroutineScope {
        val analyses: Deferred<Either<Failure, List<Analysis>>> = async {
            refreshAnalyses(
                latitude = latitude,
                longitude = longitude,
                weatherPlaceKind = weatherPlaceKind
            )
        }
        when (
            val forecasts: Either<Failure, Forecasts> = refreshForecasts(
                latitude = latitude,
                longitude = longitude,
                weatherPlaceKind = weatherPlaceKind
            )
        ) {
            is Either.Left -> {
                analyses.cancel()
                forecasts.a.asLeft()
            }
            is Either.Right -> PlaceWeather(
                forecasts = forecasts.b.forecasts,
                analyses = analyses.await().successOrNull.orEmpty(),
                fetchedAt = forecasts.b.fetchedAt
            ).asRight()
        }
    }