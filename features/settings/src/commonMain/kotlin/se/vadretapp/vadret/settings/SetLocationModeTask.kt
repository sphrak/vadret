package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface SetLocationModeTask {
    suspend operator fun invoke(isAutomaticLocation: Boolean): Either<Failure, Unit>
}