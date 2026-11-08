package se.vadretapp.vadret.setting.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val themeKey: Preferences.Key<String> = stringPreferencesKey(name = "theme")
private val languageKey: Preferences.Key<String> = stringPreferencesKey(name = "language")
private val hourFormatKey: Preferences.Key<String> = stringPreferencesKey(name = "hour_format")

internal class DefaultSettingLocalDataSource(
    private val dataStore: DataStore<Preferences>
) : SettingLocalDataSource {

    override suspend fun getTheme(): String? = get(key = themeKey)

    override fun observeTheme(): Flow<String?> = observe(key = themeKey)

    override suspend fun setTheme(value: String) = set(
        key = themeKey,
        value = value
    )

    override suspend fun getLanguage(): String? = get(key = languageKey)

    override fun observeLanguage(): Flow<String?> = observe(key = languageKey)

    override suspend fun setLanguage(value: String) = set(
        key = languageKey,
        value = value
    )

    override suspend fun getHourFormatChoice(): String? = get(key = hourFormatKey)

    override fun observeHourFormatChoice(): Flow<String?> = observe(key = hourFormatKey)

    override suspend fun setHourFormatChoice(value: String) = set(
        key = hourFormatKey,
        value = value
    )

    private suspend fun get(key: Preferences.Key<String>): String? = dataStore.data.first()[key]

    private fun observe(key: Preferences.Key<String>): Flow<String?> =
        dataStore.data
            .map { preferences: Preferences -> preferences[key] }
            .distinctUntilChanged()

    private suspend fun set(
        key: Preferences.Key<String>,
        value: String
    ) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}