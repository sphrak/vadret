package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite

internal class FakeFavoriteRepository(
    favorites: List<Favorite> = emptyList(),
    private val failure: Failure? = null
) : FavoriteRepository {
    val favorites = MutableStateFlow(value = favorites)

    override fun observeFavorites(): Flow<Either<Failure, List<Favorite>>> =
        favorites.map { favoriteList: List<Favorite> -> failure?.asLeft() ?: favoriteList.asRight() }

    override suspend fun addFavorite(
        locality: String,
        municipality: String,
        county: String,
        latitude: Double,
        longitude: Double
    ): Either<Failure, Favorite> {
        val favorite = Favorite(
            favoriteId = "favorite-$locality",
            locality = locality,
            municipality = municipality,
            county = county,
            latitude = latitude,
            longitude = longitude
        )
        favorites.update { favoriteList: List<Favorite> -> favoriteList + favorite }
        return favorite.asRight()
    }

    override suspend fun removeFavorite(favoriteId: String): Either<Failure, Unit> {
        favorites.update { favoriteList: List<Favorite> -> favoriteList.filterNot { favorite: Favorite -> favorite.favoriteId == favoriteId } }
        return Unit.asRight()
    }
}