package se.vadretapp.vadret.forecastwarning

import io.github.sphrak.either.Either
import se.vadretapp.vadret.aggregatedfeed.SeenWarningRepository
import se.vadretapp.vadret.common.Failure

internal class DefaultMarkWarningSeenTask(
    private val seenWarningRepository: SeenWarningRepository
) : MarkWarningSeenTask {

    override suspend fun invoke(warningIdentifier: String): Either<Failure, Unit> = seenWarningRepository.markSeen(warningIdentifier = warningIdentifier)
}