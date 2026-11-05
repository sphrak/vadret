package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository

interface RemoveFavoriteTask {

    suspend operator fun invoke(favoriteId: String): Either<Failure, Unit>
}

fun RemoveFavoriteTask(favoriteRepository: FavoriteRepository): RemoveFavoriteTask = DefaultRemoveFavoriteTask(favoriteRepository = favoriteRepository)