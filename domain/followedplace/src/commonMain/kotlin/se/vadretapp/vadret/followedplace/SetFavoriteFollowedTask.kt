package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.location.LocationRepository

interface SetFavoriteFollowedTask {

    suspend operator fun invoke(
        favoriteId: String,
        isFollowed: Boolean
    ): Either<Failure, Unit>
}

fun SetFavoriteFollowedTask(
    favoriteRepository: FavoriteRepository,
    locationRepository: LocationRepository
): SetFavoriteFollowedTask =
    DefaultSetFavoriteFollowedTask(
        favoriteRepository = favoriteRepository,
        locationRepository = locationRepository
    )