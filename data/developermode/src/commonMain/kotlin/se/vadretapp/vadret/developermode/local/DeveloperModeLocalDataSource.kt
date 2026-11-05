package se.vadretapp.vadret.developermode.local

import kotlinx.coroutines.flow.Flow

internal interface DeveloperModeLocalDataSource {
    fun observeIsDeveloperModeUnlocked(): Flow<Boolean>
    suspend fun unlockDeveloperMode()
}