package se.vadretapp.vadret.favorite.local

import androidx.room3.withWriteTransaction
import kotlinx.coroutines.flow.Flow

internal class DefaultFavoriteLocalDataSource(
    private val favoriteDatabase: FavoriteDatabase
) : FavoriteLocalDataSource {

    private val favoriteDao: FavoriteDao = favoriteDatabase.favoriteDao()

    override fun observeFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.observeAll()

    override suspend fun addFavorite(favoriteEntity: FavoriteEntity): FavoriteEntity =
        favoriteDatabase.withWriteTransaction {
            favoriteDao.insert(favoriteEntity = favoriteEntity)
            favoriteDao.getByPlace(
                locality = favoriteEntity.locality,
                municipality = favoriteEntity.municipality,
                county = favoriteEntity.county
            )
        }

    override suspend fun removeFavorite(favoriteId: String) {
        favoriteDatabase.withWriteTransaction {
            favoriteDao.delete(favoriteId = favoriteId)
        }
    }
}