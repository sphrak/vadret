package se.vadretapp.vadret.location

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.local.DefaultStoredLocationLocalDataSource
import se.vadretapp.vadret.location.local.DeviceLocationLocalDataSource
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation

interface LocationRepository {
    suspend fun getDeviceLocation(): Either<Failure, Location>
    suspend fun isAutomatic(): Either<Failure, Boolean>
    suspend fun setAutomatic(isAutomatic: Boolean): Either<Failure, Unit>
    suspend fun getStoredLocation(): Either<Failure, StoredLocation>
    fun observeLocationPreference(): Flow<Either<Failure, LocationPreference>>
    suspend fun selectStoredLocation(storedLocation: StoredLocation): Either<Failure, Unit>
}

internal fun LocationRepository(
    deviceLocationLocalDataSource: DeviceLocationLocalDataSource,
    dataStore: DataStore<Preferences>
): LocationRepository =
    DefaultLocationRepository(
        deviceLocationLocalDataSource = deviceLocationLocalDataSource,
        storedLocationLocalDataSource = DefaultStoredLocationLocalDataSource(dataStore = dataStore)
    )