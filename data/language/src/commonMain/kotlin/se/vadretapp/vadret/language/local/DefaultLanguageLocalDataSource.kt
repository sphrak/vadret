package se.vadretapp.vadret.language.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val languageKey: Preferences.Key<String> = stringPreferencesKey(name = "language")

internal class DefaultLanguageLocalDataSource(
    private val dataStore: DataStore<Preferences>
) : LanguageLocalDataSource {

    override suspend fun getLanguage(): String? = dataStore.data.first()[languageKey]

    override fun observeLanguage(): Flow<String?> =
        dataStore.data
            .map { preferences: Preferences -> preferences[languageKey] }
            .distinctUntilChanged()

    override suspend fun setLanguage(value: String) {
        dataStore.edit { preferences ->
            preferences[languageKey] = value
        }
    }
}