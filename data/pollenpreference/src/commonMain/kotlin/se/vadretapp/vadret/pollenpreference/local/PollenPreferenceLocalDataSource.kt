package se.vadretapp.vadret.pollenpreference.local

import kotlinx.coroutines.flow.Flow

internal interface PollenPreferenceLocalDataSource {
    suspend fun getPollenPreferences(): PollenPreferencesEntity
    fun observePollenPreferences(): Flow<PollenPreferencesEntity>
    suspend fun setPollenEnabled(value: Boolean)
    suspend fun setAllergen(
        pollenId: String,
        isAllergen: Boolean
    )
    suspend fun setHibernationNoticeShown(value: Boolean)
    suspend fun setEndOfSeasonSimulated(value: Boolean)
}