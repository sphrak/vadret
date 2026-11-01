package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.pushalert.usecase.ObserveFavoritesTask

internal class FakeObserveFavoritesTask(
    favorites: List<Favorite> = emptyList(),
    private val failure: Failure? = null
) : ObserveFavoritesTask {
    val favorites = MutableStateFlow(value = favorites)

    override fun invoke(): Flow<Either<Failure, List<Favorite>>> =
        favorites.map { favoriteList: List<Favorite> -> failure?.asLeft() ?: favoriteList.asRight() }
}