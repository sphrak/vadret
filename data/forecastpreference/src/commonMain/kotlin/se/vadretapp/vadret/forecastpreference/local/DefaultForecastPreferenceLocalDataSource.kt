package se.vadretapp.vadret.forecastpreference.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val isHourlyForecastEnabledKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "is_hourly_forecast_enabled")
private val isExpandOnceEnabledKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "is_expand_once_enabled")
private val isAuroraEnabledKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "is_aurora_enabled")

internal class DefaultForecastPreferenceLocalDataSource(
    private val dataStore: DataStore<Preferences>
) : ForecastPreferenceLocalDataSource {

    override suspend fun getForecastPreferences(): ForecastPreferencesEntity = dataStore.data.first().toForecastPreferencesEntity()

    override fun observeForecastPreferences(): Flow<ForecastPreferencesEntity> =
        dataStore.data
            .map { preferences: Preferences -> preferences.toForecastPreferencesEntity() }
            .distinctUntilChanged()

    override suspend fun setHourlyForecastEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[isHourlyForecastEnabledKey] = value
        }
    }

    override suspend fun setExpandOnceEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[isExpandOnceEnabledKey] = value
        }
    }

    override suspend fun setAuroraEnabled(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[isAuroraEnabledKey] = value
        }
    }

    private fun Preferences.toForecastPreferencesEntity(): ForecastPreferencesEntity =
        ForecastPreferencesEntity(
            isHourlyForecastEnabled = this[isHourlyForecastEnabledKey],
            isExpandOnceEnabled = this[isExpandOnceEnabledKey],
            isAuroraEnabled = this[isAuroraEnabledKey]
        )
}