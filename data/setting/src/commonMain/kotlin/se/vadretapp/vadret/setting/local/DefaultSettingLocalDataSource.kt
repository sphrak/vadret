package se.vadretapp.vadret.setting.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val themeKey: Preferences.Key<String> = stringPreferencesKey(name = "theme")
private val languageKey: Preferences.Key<String> = stringPreferencesKey(name = "language")
private val hourFormatKey: Preferences.Key<String> = stringPreferencesKey(name = "hour_format")
private val nativeNavigationKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "native_navigation")

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

    override suspend fun getNativeNavigationEnabled(): Boolean? = get(key = nativeNavigationKey)

    override fun observeNativeNavigationEnabled(): Flow<Boolean?> = observe(key = nativeNavigationKey)

    override suspend fun setNativeNavigationEnabled(value: Boolean) = set(
        key = nativeNavigationKey,
        value = value
    )

    private suspend fun <T> get(key: Preferences.Key<T>): T? = dataStore.data.first()[key]

    private fun <T> observe(key: Preferences.Key<T>): Flow<T?> =
        dataStore.data
            .map { preferences: Preferences -> preferences[key] }
            .distinctUntilChanged()

    private suspend fun <T> set(
        key: Preferences.Key<T>,
        value: T
    ) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}