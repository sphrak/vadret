package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface SetExpandOnceTask {
    suspend operator fun invoke(isExpandOnceEnabled: Boolean): Either<Failure, Unit>
}