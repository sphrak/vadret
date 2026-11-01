package se.vadretapp.vadret.location.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val isAutomaticKey: Preferences.Key<Boolean> = booleanPreferencesKey(name = "is_automatic_location")
private val localityKey: Preferences.Key<String> = stringPreferencesKey(name = "locality")
private val municipalityKey: Preferences.Key<String> = stringPreferencesKey(name = "municipality")
private val countyKey: Preferences.Key<String> = stringPreferencesKey(name = "county")
private val latitudeKey: Preferences.Key<Double> = doublePreferencesKey(name = "latitude")
private val longitudeKey: Preferences.Key<Double> = doublePreferencesKey(name = "longitude")

private const val DEFAULT_IS_AUTOMATIC = true
private const val DEFAULT_LOCALITY = "Stockholm"
private const val DEFAULT_MUNICIPALITY = "Stockholm"
private const val DEFAULT_COUNTY = "Stockholm"
private const val DEFAULT_LATITUDE = 59.3293
private const val DEFAULT_LONGITUDE = 18.0686

internal class DefaultStoredLocationLocalDataSource(
    private val dataStore: DataStore<Preferences>
) : StoredLocationLocalDataSource {

    override suspend fun isAutomatic(): Boolean = dataStore.data.first()[isAutomaticKey] ?: DEFAULT_IS_AUTOMATIC

    override suspend fun setAutomatic(isAutomatic: Boolean) {
        dataStore.edit { preferences ->
            preferences[isAutomaticKey] = isAutomatic
        }
    }

    override suspend fun getStoredLocation(): StoredLocationEntity = dataStore.data.first().toStoredLocationEntity()

    override fun observeLocationPreference(): Flow<LocationPreferenceEntity> =
        dataStore.data
            .map { preferences: Preferences ->
                LocationPreferenceEntity(
                    isAutomatic = preferences[isAutomaticKey] ?: DEFAULT_IS_AUTOMATIC,
                    storedLocationEntity = preferences.toStoredLocationEntity()
                )
            }.distinctUntilChanged()

    override suspend fun selectStoredLocation(storedLocationEntity: StoredLocationEntity) {
        dataStore.edit { preferences ->
            preferences[isAutomaticKey] = false
            preferences[localityKey] = storedLocationEntity.locality
            preferences[municipalityKey] = storedLocationEntity.municipality
            preferences[countyKey] = storedLocationEntity.county
            preferences[latitudeKey] = storedLocationEntity.latitude
            preferences[longitudeKey] = storedLocationEntity.longitude
        }
    }
}

private fun Preferences.toStoredLocationEntity(): StoredLocationEntity =
    StoredLocationEntity(
        locality = this[localityKey] ?: DEFAULT_LOCALITY,
        municipality = this[municipalityKey] ?: DEFAULT_MUNICIPALITY,
        county = this[countyKey] ?: DEFAULT_COUNTY,
        latitude = this[latitudeKey] ?: DEFAULT_LATITUDE,
        longitude = this[longitudeKey] ?: DEFAULT_LONGITUDE
    )