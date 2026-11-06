package se.vadretapp.vadret.radar.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.wind.model.WindForecast

internal interface ObserveWindForecastTask {

    operator fun invoke(): Flow<Either<Failure, WindForecast>>
}