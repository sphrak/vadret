package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface UnlockDeveloperModeTask {
    suspend operator fun invoke(): Either<Failure, Unit>
}