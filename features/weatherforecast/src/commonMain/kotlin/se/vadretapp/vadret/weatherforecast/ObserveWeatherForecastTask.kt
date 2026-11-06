package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast

internal interface ObserveWeatherForecastTask {

    operator fun invoke(isRefresh: Boolean): Flow<Either<Failure, PlaceForecast>>
}