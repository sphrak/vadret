package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.nominatim.NominatimRepository
import se.vadretapp.vadret.nominatim.model.Locality
import se.vadretapp.vadret.weatherforecast.extension.observeRefreshedWeather
import se.vadretapp.vadret.weatherforecast.extension.toPlaceForecast
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast
import se.vadretapp.vadret.weatherforecast.model.RefreshedWeather

internal class DefaultObservePlaceForecastTask(
    private val weatherForecastRepository: WeatherForecastRepository,
    private val nominatimRepository: NominatimRepository
) : ObservePlaceForecastTask {

    override fun invoke(
        latitude: Double,
        longitude: Double,
        isRefresh: Boolean
    ): Flow<Either<Failure, PlaceForecast>> =
        channelFlow {
            val locality: Deferred<Locality?> = async {
                getLocality(
                    latitude = latitude,
                    longitude = longitude
                )
            }
            weatherForecastRepository
                .observeRefreshedWeather(
                    latitude = latitude,
                    longitude = longitude,
                    isRefresh = isRefresh
                ).collect { refreshedWeather: Either<Failure, RefreshedWeather> ->
                    when (refreshedWeather) {
                        is Either.Left -> send(element = refreshedWeather.a.asLeft())
                        is Either.Right -> {
                            val foundLocality: Locality? = locality.await()
                            send(
                                element = refreshedWeather.b.toPlaceForecast(
                                    placeName = foundLocality?.name,
                                    municipality = foundLocality?.municipality,
                                    county = foundLocality?.county,
                                    latitude = latitude,
                                    longitude = longitude
                                ).asRight()
                            )
                        }
                    }
                }
        }

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