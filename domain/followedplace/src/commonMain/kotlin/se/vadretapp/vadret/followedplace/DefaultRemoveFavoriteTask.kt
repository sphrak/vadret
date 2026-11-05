package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository

internal class DefaultRemoveFavoriteTask(
    private val favoriteRepository: FavoriteRepository
) : RemoveFavoriteTask {

    override suspend fun invoke(favoriteId: String): Either<Failure, Unit> = favoriteRepository.removeFavorite(favoriteId = favoriteId)
}