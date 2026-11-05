package se.vadretapp.vadret.favorite.local

import androidx.room3.withWriteTransaction
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.favorite.exception.FavoriteLimitException

internal class DefaultFavoriteLocalDataSource(
    private val favoriteDatabase: FavoriteDatabase
) : FavoriteLocalDataSource {

    private val favoriteDao: FavoriteDao = favoriteDatabase.favoriteDao()

    override fun observeFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.observeAll()

    // The limit is counted in the same transaction that adds, so two quick adds cannot both pass it.
    override suspend fun addFavorite(
        favoriteEntity: FavoriteEntity,
        maximumFavorites: Int
    ): FavoriteEntity =
        favoriteDatabase.withWriteTransaction {
            val storedFavoriteEntity: FavoriteEntity? = favoriteDao.getByPlace(
                locality = favoriteEntity.locality,
                municipality = favoriteEntity.municipality,
                county = favoriteEntity.county
            )
            if (storedFavoriteEntity != null) {
                storedFavoriteEntity
            } else if (favoriteDao.count() >= maximumFavorites) {
                throw FavoriteLimitException()
            } else {
                favoriteDao.insert(favoriteEntity = favoriteEntity)
                favoriteEntity
            }
        }

    override suspend fun removeFavorite(favoriteId: String) {
        favoriteDatabase.withWriteTransaction {
            favoriteDao.delete(favoriteId = favoriteId)
        }
    }
}