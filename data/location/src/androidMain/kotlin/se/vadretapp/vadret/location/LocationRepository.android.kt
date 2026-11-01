package se.vadretapp.vadret.location

import android.location.LocationManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineDispatcher
import se.vadretapp.vadret.location.local.DefaultDeviceLocationLocalDataSource

fun LocationRepository(
    locationManager: LocationManager,
    dataStore: DataStore<Preferences>,
    coroutineDispatcher: CoroutineDispatcher
): LocationRepository =
    LocationRepository(
        deviceLocationLocalDataSource = DefaultDeviceLocationLocalDataSource(
            locationManager = locationManager,
            coroutineDispatcher = coroutineDispatcher
        ),
        dataStore = dataStore
    )