package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.extension.toStoredLocation
import se.vadretapp.vadret.location.LocationRepository

internal class DefaultSelectFavoriteTask(
    private val locationRepository: LocationRepository
) : SelectFavoriteTask {

    override suspend fun invoke(favorite: Favorite): Either<Failure, Unit> = locationRepository.selectStoredLocation(storedLocation = favorite.toStoredLocation())
}