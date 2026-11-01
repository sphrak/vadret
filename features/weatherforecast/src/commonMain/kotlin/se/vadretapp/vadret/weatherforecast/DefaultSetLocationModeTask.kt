package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.LocationRepository

internal class DefaultSetLocationModeTask(
    private val locationRepository: LocationRepository
) : SetLocationModeTask {

    override suspend fun invoke(isAutomatic: Boolean): Either<Failure, Unit> = locationRepository.setAutomatic(isAutomatic = isAutomatic)
}