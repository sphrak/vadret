package se.vadretapp.vadret.pollenpreference.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val isPollenEnabledKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "is_pollen_enabled")
private val allergenPollenIdsKey: Preferences.Key<Set<String>> = stringSetPreferencesKey(name = "allergen_pollen_ids")
private val isPollenHibernationNoticeShownKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "is_pollen_hibernation_notice_shown")
private val isPollenEndOfSeasonSimulatedKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "is_pollen_end_of_season_simulated")

internal class DefaultPollenPreferenceLocalDataSource(
    private val dataStore: DataStore<Preferences>
) : PollenPreferenceLocalDataSource {

    override suspend fun getPollenPreferences(): PollenPreferencesEntity = dataStore.data.first().toPollenPreferencesEntity()

    override fun observePollenPreferences(): Flow<PollenPreferencesEntity> =
        dataStore.data
            .map { preferences: Preferences -> preferences.toPollenPreferencesEntity() }
            .distinctUntilChanged()

    override suspend fun setPollenEnabled(value: Boolean) {
        dataStore.edit { preferences: MutablePreferences ->
            preferences[isPollenEnabledKey] = value
        }
    }

    override suspend fun setAllergen(
        pollenId: String,
        isAllergen: Boolean
    ) {
        dataStore.edit { preferences: MutablePreferences ->
            val allergenPollenIds: Set<String> = preferences[allergenPollenIdsKey].orEmpty()
            preferences[allergenPollenIdsKey] = if (isAllergen) {
                allergenPollenIds + pollenId
            } else {
                allergenPollenIds - pollenId
            }
        }
    }

    override suspend fun setHibernationNoticeShown(value: Boolean) {
        dataStore.edit { preferences: MutablePreferences ->
            preferences[isPollenHibernationNoticeShownKey] = value
        }
    }

    override suspend fun setEndOfSeasonSimulated(value: Boolean) {
        dataStore.edit { preferences: MutablePreferences ->
            preferences[isPollenEndOfSeasonSimulatedKey] = value
        }
    }

    private fun Preferences.toPollenPreferencesEntity(): PollenPreferencesEntity =
        PollenPreferencesEntity(
            isPollenEnabled = this[isPollenEnabledKey],
            allergenPollenIds = this[allergenPollenIdsKey],
            isHibernationNoticeShown = this[isPollenHibernationNoticeShownKey],
            isEndOfSeasonSimulated = this[isPollenEndOfSeasonSimulatedKey]
        )
}