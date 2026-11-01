package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.aggregatedfeed.FeedRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.warning.model.WarningPlace

interface ObserveUnseenWarningsTask {

    operator fun invoke(
        warningPlace: WarningPlace,
        isRefresh: Boolean
    ): Flow<Either<Failure, List<AggregatedFeed>>>
}

fun ObserveUnseenWarningsTask(feedRepository: FeedRepository): ObserveUnseenWarningsTask = DefaultObserveUnseenWarningsTask(feedRepository = feedRepository)