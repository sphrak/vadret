package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite

internal class DefaultObserveFavoritesTask(
    private val favoriteRepository: FavoriteRepository
) : ObserveFavoritesTask {
    override fun invoke(): Flow<Either<Failure, List<Favorite>>> = favoriteRepository.observeFavorites()
}