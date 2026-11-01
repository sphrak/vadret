package se.vadretapp.vadret.location.local

import kotlinx.coroutines.flow.Flow

internal interface StoredLocationLocalDataSource {
    suspend fun isAutomatic(): Boolean
    suspend fun setAutomatic(isAutomatic: Boolean)
    suspend fun getStoredLocation(): StoredLocationEntity
    fun observeLocationPreference(): Flow<LocationPreferenceEntity>
    suspend fun selectStoredLocation(storedLocationEntity: StoredLocationEntity)
}