package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.wind.model.WindForecast

internal interface GetWindForecastTask {

    suspend operator fun invoke(): Either<Failure, WindForecast>
}