package se.vadretapp.vadret.radar.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.model.Location

internal interface GetDeviceLocationTask {

    suspend operator fun invoke(): Either<Failure, Location>
}