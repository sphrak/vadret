package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface SetAuroraTask {
    suspend operator fun invoke(isAuroraEnabled: Boolean): Either<Failure, Unit>
}