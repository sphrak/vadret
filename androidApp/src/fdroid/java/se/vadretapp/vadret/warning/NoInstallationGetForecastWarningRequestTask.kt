package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.aggregatedfeed.model.ForecastWarningRequest
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.warning.usecase.GetForecastWarningRequestTask

internal class NoInstallationGetForecastWarningRequestTask : GetForecastWarningRequestTask {

    override suspend fun invoke(): Either<Failure, ForecastWarningRequest?> = null.asRight()
}