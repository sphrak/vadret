package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import se.vadretapp.vadret.aggregatedfeed.FeedRepository
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure

internal class DefaultGetWarningsTask(
    private val feedRepository: FeedRepository
) : GetWarningsTask {

    override suspend fun invoke(): Either<Failure, List<AggregatedFeed>> = feedRepository.getAggregatedFeed()
}