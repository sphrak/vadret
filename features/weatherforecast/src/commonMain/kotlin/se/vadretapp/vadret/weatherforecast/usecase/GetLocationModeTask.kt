package se.vadretapp.vadret.weatherforecast.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface GetLocationModeTask {

    suspend operator fun invoke(): Either<Failure, Boolean>
}