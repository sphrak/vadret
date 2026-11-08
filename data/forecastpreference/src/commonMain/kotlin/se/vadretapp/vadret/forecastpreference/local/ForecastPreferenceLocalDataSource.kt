package se.vadretapp.vadret.forecastpreference.local

import kotlinx.coroutines.flow.Flow

internal interface ForecastPreferenceLocalDataSource {
    suspend fun getForecastPreferences(): ForecastPreferencesEntity
    fun observeForecastPreferences(): Flow<ForecastPreferencesEntity>
    suspend fun setHourlyForecastEnabled(value: Boolean)
    suspend fun setExpandOnceEnabled(value: Boolean)
    suspend fun setAuroraEnabled(value: Boolean)
}