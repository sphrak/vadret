package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface UnlockDeveloperModeTask {
    suspend operator fun invoke(): Either<Failure, Unit>
}