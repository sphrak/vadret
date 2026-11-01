package se.vadretapp.vadret.aggregatedfeed.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SeenWarningDao {

    @Query(
        value = """
        SELECT
            warning_identifier,
            seen_at_epoch_millis
        FROM seen_warning
        ORDER BY seen_at_epoch_millis DESC, warning_identifier ASC
        """
    )
    fun observeAll(): Flow<List<SeenWarningEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(seenWarningEntity: SeenWarningEntity)
}