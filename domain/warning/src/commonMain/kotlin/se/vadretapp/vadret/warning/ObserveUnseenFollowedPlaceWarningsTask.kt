package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure

interface ObserveUnseenFollowedPlaceWarningsTask {

    operator fun invoke(isRefresh: Boolean): Flow<Either<Failure, List<AggregatedFeed>>>
}