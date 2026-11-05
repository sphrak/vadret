package se.vadretapp.vadret.favorite.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface FavoriteDao {

    @Query(
        value = """
        SELECT
            favorite_id,
            locality,
            municipality,
            county,
            latitude,
            longitude,
            created_at
        FROM favorite
        ORDER BY created_at ASC, favorite_id ASC
        """
    )
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query(
        value = """
        SELECT
            favorite_id,
            locality,
            municipality,
            county,
            latitude,
            longitude,
            created_at
        FROM favorite
        WHERE locality = :locality
            AND municipality = :municipality
            AND county = :county
        """
    )
    suspend fun getByPlace(
        locality: String,
        municipality: String,
        county: String
    ): FavoriteEntity?

    @Query(value = "SELECT COUNT(favorite_id) FROM favorite")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(favoriteEntity: FavoriteEntity)

    @Query(value = "DELETE FROM favorite WHERE favorite_id = :favoriteId")
    suspend fun delete(favoriteId: String)
}