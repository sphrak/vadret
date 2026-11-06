package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast

internal interface ObservePlaceForecastTask {

    operator fun invoke(
        latitude: Double,
        longitude: Double,
        isRefresh: Boolean
    ): Flow<Either<Failure, PlaceForecast>>
}