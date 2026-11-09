package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface SetNativeNavigationTask {
    suspend operator fun invoke(isNativeNavigationEnabled: Boolean): Either<Failure, Unit>
}