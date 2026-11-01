package se.vadretapp.vadret.forecastwarning

import io.github.sphrak.either.Either
import se.vadretapp.vadret.aggregatedfeed.FeedRepository
import se.vadretapp.vadret.common.Failure

internal class DefaultMarkWarningSeenTask(
    private val feedRepository: FeedRepository
) : MarkWarningSeenTask {

    override suspend fun invoke(warningIdentifier: String): Either<Failure, Unit> = feedRepository.markSeen(warningIdentifier = warningIdentifier)
}