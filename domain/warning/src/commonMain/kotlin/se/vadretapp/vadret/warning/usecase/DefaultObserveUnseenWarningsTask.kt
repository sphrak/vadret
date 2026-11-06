package se.vadretapp.vadret.warning.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.aggregatedfeed.FeedRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.warning.model.WarningPlace
import kotlin.time.Clock

internal class DefaultObserveUnseenWarningsTask(
    private val feedRepository: FeedRepository,
    private val clock: Clock
) : ObserveUnseenWarningsTask {

    override fun invoke(
        warningPlace: WarningPlace,
        isRefresh: Boolean
    ): Flow<Either<Failure, List<AggregatedFeed>>> =
        feedRepository.observeUnseenWarnings(
            warningPlaces = listOf(warningPlace),
            isRefresh = isRefresh,
            clock = clock
        )
}