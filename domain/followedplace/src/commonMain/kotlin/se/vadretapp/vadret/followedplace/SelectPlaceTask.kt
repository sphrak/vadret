package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.place.model.Place

interface SelectPlaceTask {

    suspend operator fun invoke(place: Place): Either<Failure, Unit>
}

fun SelectPlaceTask(
    favoriteRepository: FavoriteRepository,
    locationRepository: LocationRepository
): SelectPlaceTask =
    DefaultSelectPlaceTask(
        favoriteRepository = favoriteRepository,
        locationRepository = locationRepository
    )