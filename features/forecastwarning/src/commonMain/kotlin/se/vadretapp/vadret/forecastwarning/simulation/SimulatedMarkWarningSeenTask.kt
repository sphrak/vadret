package se.vadretapp.vadret.forecastwarning.simulation

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastwarning.MarkWarningSeenTask

// A simulated story is made up, so seeing it leaves the warnings that were really seen as they are.
internal class SimulatedMarkWarningSeenTask : MarkWarningSeenTask {

    override suspend fun invoke(warningIdentifier: String): Either<Failure, Unit> = Unit.asRight()
}