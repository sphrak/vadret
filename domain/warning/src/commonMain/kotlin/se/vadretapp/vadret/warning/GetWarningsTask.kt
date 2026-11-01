package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import se.vadretapp.vadret.aggregatedfeed.FeedRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure

interface GetWarningsTask {

    suspend operator fun invoke(isRefresh: Boolean): Either<Failure, List<AggregatedFeed>>
}

fun GetWarningsTask(feedRepository: FeedRepository): GetWarningsTask = DefaultGetWarningsTask(feedRepository = feedRepository)