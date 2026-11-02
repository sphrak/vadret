package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast

internal interface GetPlaceForecastTask {

    suspend operator fun invoke(
        latitude: Double,
        longitude: Double
    ): Either<Failure, PlaceForecast>
}