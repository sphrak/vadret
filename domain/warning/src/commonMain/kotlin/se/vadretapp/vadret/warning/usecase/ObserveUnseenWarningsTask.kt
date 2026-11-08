package se.vadretapp.vadret.warning.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.aggregatedfeed.SeenWarningRepository
import se.vadretapp.vadret.aggregatedfeed.WarningsRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.warning.model.WarningPlace
import kotlin.time.Clock

interface ObserveUnseenWarningsTask {

    operator fun invoke(
        warningPlace: WarningPlace,
        isRefresh: Boolean
    ): Flow<Either<Failure, List<AggregatedFeed>>>
}

fun ObserveUnseenWarningsTask(
    warningsRepository: WarningsRepository,
    seenWarningRepository: SeenWarningRepository,
    getForecastWarningRequestTask: GetForecastWarningRequestTask,
    clock: Clock
): ObserveUnseenWarningsTask =
    DefaultObserveUnseenWarningsTask(
        warningsRepository = warningsRepository,
        seenWarningRepository = seenWarningRepository,
        getForecastWarningRequestTask = getForecastWarningRequestTask,
        clock = clock
    )