package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface SetEndOfSeasonSimulatedTask {
    suspend operator fun invoke(isEndOfSeasonSimulated: Boolean): Either<Failure, Unit>
}