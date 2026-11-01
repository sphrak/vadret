package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.weatherforecast.extension.toStoredLocation

internal class DefaultSelectFavoriteTask(
    private val locationRepository: LocationRepository
) : SelectFavoriteTask {

    override suspend fun invoke(favorite: Favorite): Either<Failure, Unit> = locationRepository.selectStoredLocation(storedLocation = favorite.toStoredLocation())
}