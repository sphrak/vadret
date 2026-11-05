package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.location.LocationRepository

interface AddFavoriteTask {

    suspend operator fun invoke(
        locality: String,
        municipality: String,
        county: String,
        latitude: Double,
        longitude: Double
    ): Either<Failure, Favorite>
}

fun AddFavoriteTask(
    favoriteRepository: FavoriteRepository,
    locationRepository: LocationRepository
): AddFavoriteTask =
    DefaultAddFavoriteTask(
        favoriteRepository = favoriteRepository,
        locationRepository = locationRepository
    )