package se.vadretapp.vadret.developermode

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.developermode.local.DefaultDeveloperModeLocalDataSource

interface DeveloperModeRepository {
    fun observeIsDeveloperModeUnlocked(): Flow<Either<Failure, Boolean>>
    suspend fun unlockDeveloperMode(): Either<Failure, Unit>
}

fun DeveloperModeRepository(dataStore: DataStore<Preferences>): DeveloperModeRepository =
    DefaultDeveloperModeRepository(
        developerModeLocalDataSource = DefaultDeveloperModeLocalDataSource(dataStore = dataStore)
    )