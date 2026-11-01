package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.Location

internal class DefaultGetDeviceLocationTask(
    private val locationRepository: LocationRepository
) : GetDeviceLocationTask {

    override suspend fun invoke(): Either<Failure, Location> = locationRepository.getDeviceLocation()
}