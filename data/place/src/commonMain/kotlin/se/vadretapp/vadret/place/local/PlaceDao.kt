package se.vadretapp.vadret.place.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
internal interface PlaceDao {

    @Query(
        value = """
        SELECT
            locality,
            municipality,
            county,
            latitude,
            longitude,
            population,
            folded_locality
        FROM place
        WHERE folded_locality LIKE :prefixPattern ESCAPE '\'
            OR folded_locality LIKE :wordPattern ESCAPE '\'
        ORDER BY
            CASE
                WHEN folded_locality = :foldedQuery THEN 0
                WHEN folded_locality LIKE :prefixPattern ESCAPE '\' THEN 1
                ELSE 2
            END ASC,
            population DESC,
            locality ASC,
            municipality ASC
        LIMIT :limit
        """
    )
    suspend fun search(
        foldedQuery: String,
        prefixPattern: String,
        wordPattern: String,
        limit: Int
    ): List<PlaceEntity>

    @Query(value = "SELECT COUNT(seed_version) FROM place_seed WHERE seed_version = :seedVersion")
    suspend fun countSeeds(seedVersion: String): Int

    @Query(value = "DELETE FROM place")
    suspend fun deletePlaces()

    @Query(value = "DELETE FROM place_seed")
    suspend fun deleteSeeds()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPlaces(placeEntities: List<PlaceEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSeed(placeSeedEntity: PlaceSeedEntity)
}