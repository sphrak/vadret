package se.vadretapp.vadret.warning.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.aggregatedfeed.SeenWarningRepository
import se.vadretapp.vadret.aggregatedfeed.WarningsRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.warning.model.WarningPlace
import kotlin.time.Clock

internal class DefaultObserveUnseenWarningsTask(
    private val warningsRepository: WarningsRepository,
    private val seenWarningRepository: SeenWarningRepository,
    private val getForecastWarningRequestTask: GetForecastWarningRequestTask,
    private val clock: Clock
) : ObserveUnseenWarningsTask {

    override fun invoke(
        warningPlace: WarningPlace,
        isRefresh: Boolean
    ): Flow<Either<Failure, List<AggregatedFeed>>> =
        warningsRepository.observeUnseenWarnings(
            seenWarningRepository = seenWarningRepository,
            warningPlaces = listOf(warningPlace),
            isRefresh = isRefresh,
            getForecastWarningRequestTask = getForecastWarningRequestTask,
            clock = clock
        )
}