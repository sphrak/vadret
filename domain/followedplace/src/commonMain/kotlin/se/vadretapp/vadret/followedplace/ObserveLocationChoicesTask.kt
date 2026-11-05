package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.followedplace.model.LocationChoices
import se.vadretapp.vadret.location.LocationRepository

interface ObserveLocationChoicesTask {

    operator fun invoke(): Flow<Either<Failure, LocationChoices>>
}

fun ObserveLocationChoicesTask(
    favoriteRepository: FavoriteRepository,
    locationRepository: LocationRepository
): ObserveLocationChoicesTask =
    DefaultObserveLocationChoicesTask(
        favoriteRepository = favoriteRepository,
        locationRepository = locationRepository
    )