package se.vadretapp.vadret.aggregatedfeed.local

import androidx.room3.withWriteTransaction
import kotlinx.coroutines.flow.Flow

internal class DefaultSeenWarningLocalDataSource(
    private val aggregatedFeedDatabase: AggregatedFeedDatabase
) : SeenWarningLocalDataSource {

    private val seenWarningDao: SeenWarningDao = aggregatedFeedDatabase.seenWarningDao()

    override fun observeSeenWarnings(): Flow<List<SeenWarningEntity>> = seenWarningDao.observeAll()

    override suspend fun markSeen(seenWarningEntity: SeenWarningEntity) {
        aggregatedFeedDatabase.withWriteTransaction {
            seenWarningDao.insert(seenWarningEntity = seenWarningEntity)
        }
    }
}