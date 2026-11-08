package se.vadretapp.vadret.warning.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.aggregatedfeed.FeedRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure

interface GetWarningsTask {

    suspend operator fun invoke(): Either<Failure, List<AggregatedFeed>>
}

fun GetWarningsTask(
    feedRepository: FeedRepository,
    getForecastWarningRequestTask: GetForecastWarningRequestTask
): GetWarningsTask =
    DefaultGetWarningsTask(
        feedRepository = feedRepository,
        getForecastWarningRequestTask = getForecastWarningRequestTask
    )