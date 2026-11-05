package se.vadretapp.vadret.favorite.local

import kotlinx.coroutines.flow.Flow

internal interface FavoriteLocalDataSource {
    fun observeFavorites(): Flow<List<FavoriteEntity>>
    suspend fun addFavorite(
        favoriteEntity: FavoriteEntity,
        maximumFavorites: Int
    ): FavoriteEntity
    suspend fun removeFavorite(favoriteId: String)
}