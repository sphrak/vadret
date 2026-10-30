package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface SetPollenEnabledTask {
    suspend operator fun invoke(isPollenEnabled: Boolean): Either<Failure, Unit>
}