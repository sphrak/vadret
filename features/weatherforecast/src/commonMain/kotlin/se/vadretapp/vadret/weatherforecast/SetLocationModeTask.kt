package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface SetLocationModeTask {
    suspend operator fun invoke(isAutomatic: Boolean): Either<Failure, Unit>
}