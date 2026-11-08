package se.vadretapp.vadret.warning.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.aggregatedfeed.model.ForecastWarningRequest
import se.vadretapp.vadret.common.Failure

interface GetForecastWarningRequestTask {

    suspend operator fun invoke(): Either<Failure, ForecastWarningRequest?>
}