package se.vadretapp.vadret.theme.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val themeKey: Preferences.Key<String> = stringPreferencesKey(name = "theme")

internal class DefaultThemeLocalDataSource(
    private val dataStore: DataStore<Preferences>
) : ThemeLocalDataSource {

    override suspend fun getTheme(): String? = dataStore.data.first()[themeKey]

    override fun observeTheme(): Flow<String?> =
        dataStore.data
            .map { preferences: Preferences -> preferences[themeKey] }
            .distinctUntilChanged()

    override suspend fun setTheme(value: String) {
        dataStore.edit { preferences ->
            preferences[themeKey] = value
        }
    }
}