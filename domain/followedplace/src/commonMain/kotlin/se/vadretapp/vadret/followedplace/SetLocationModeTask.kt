package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.location.LocationRepository

interface SetLocationModeTask {
    suspend operator fun invoke(isAutomatic: Boolean): Either<Failure, Unit>
}

fun SetLocationModeTask(
    locationRepository: LocationRepository,
    favoriteRepository: FavoriteRepository
): SetLocationModeTask =
    DefaultSetLocationModeTask(
        locationRepository = locationRepository,
        favoriteRepository = favoriteRepository
    )