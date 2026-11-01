package se.vadretapp.vadret.aggregatedfeed.local

import kotlinx.coroutines.flow.Flow

internal interface SeenWarningLocalDataSource {
    fun observeSeenWarnings(): Flow<List<SeenWarningEntity>>
    suspend fun markSeen(seenWarningEntity: SeenWarningEntity)
}