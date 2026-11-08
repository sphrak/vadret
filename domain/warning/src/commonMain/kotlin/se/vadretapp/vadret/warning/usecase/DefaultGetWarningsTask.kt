package se.vadretapp.vadret.warning.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.aggregatedfeed.WarningsRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure

internal class DefaultGetWarningsTask(
    private val warningsRepository: WarningsRepository,
    private val getForecastWarningRequestTask: GetForecastWarningRequestTask
) : GetWarningsTask {

    override suspend fun invoke(): Either<Failure, List<AggregatedFeed>> =
        warningsRepository.getAggregatedFeed(forecastWarningRequest = getForecastWarningRequestTask.forecastWarningRequestOrNull())
}