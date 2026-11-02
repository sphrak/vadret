package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.StoredLocation

internal class DefaultGetStoredLocationTask(
    private val locationRepository: LocationRepository
) : GetStoredLocationTask {

    override suspend fun invoke(): Either<Failure, StoredLocation> = locationRepository.getStoredLocation()
}