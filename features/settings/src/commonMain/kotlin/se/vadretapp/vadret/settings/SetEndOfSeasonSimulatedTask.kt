package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface SetEndOfSeasonSimulatedTask {
    suspend operator fun invoke(isEndOfSeasonSimulated: Boolean): Either<Failure, Unit>
}