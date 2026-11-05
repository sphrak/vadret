package se.vadretapp.vadret.developermode.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val isDeveloperModeUnlockedKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "is_developer_mode_unlocked")

private const val DEFAULT_IS_DEVELOPER_MODE_UNLOCKED = false

internal class DefaultDeveloperModeLocalDataSource(
    private val dataStore: DataStore<Preferences>
) : DeveloperModeLocalDataSource {

    override fun observeIsDeveloperModeUnlocked(): Flow<Boolean> =
        dataStore.data
            .map { preferences: Preferences -> preferences[isDeveloperModeUnlockedKey] ?: DEFAULT_IS_DEVELOPER_MODE_UNLOCKED }
            .distinctUntilChanged()

    override suspend fun unlockDeveloperMode() {
        dataStore.edit { preferences ->
            preferences[isDeveloperModeUnlockedKey] = true
        }
    }
}