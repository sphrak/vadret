package se.vadretapp.vadret.warning.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.aggregatedfeed.WarningsRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure

interface GetWarningsTask {

    suspend operator fun invoke(): Either<Failure, List<AggregatedFeed>>
}

fun GetWarningsTask(
    warningsRepository: WarningsRepository,
    getForecastWarningRequestTask: GetForecastWarningRequestTask
): GetWarningsTask =
    DefaultGetWarningsTask(
        warningsRepository = warningsRepository,
        getForecastWarningRequestTask = getForecastWarningRequestTask
    )