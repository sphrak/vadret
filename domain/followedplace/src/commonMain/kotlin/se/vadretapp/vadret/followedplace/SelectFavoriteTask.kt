package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.location.LocationRepository

interface SelectFavoriteTask {

    suspend operator fun invoke(favorite: Favorite): Either<Failure, Unit>
}

fun SelectFavoriteTask(locationRepository: LocationRepository): SelectFavoriteTask = DefaultSelectFavoriteTask(locationRepository = locationRepository)