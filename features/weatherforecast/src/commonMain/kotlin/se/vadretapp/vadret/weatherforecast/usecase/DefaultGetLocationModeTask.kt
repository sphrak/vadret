package se.vadretapp.vadret.weatherforecast.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.LocationRepository

internal class DefaultGetLocationModeTask(
    private val locationRepository: LocationRepository
) : GetLocationModeTask {

    override suspend fun invoke(): Either<Failure, Boolean> = locationRepository.isAutomatic()
}