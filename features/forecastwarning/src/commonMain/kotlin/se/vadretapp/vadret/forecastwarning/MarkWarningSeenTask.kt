package se.vadretapp.vadret.forecastwarning

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface MarkWarningSeenTask {

    suspend operator fun invoke(warningIdentifier: String): Either<Failure, Unit>
}