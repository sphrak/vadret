package se.vadretapp.vadret.location

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineDispatcher
import se.vadretapp.vadret.location.local.DefaultDeviceLocationLocalDataSource

fun LocationRepository(
    dataStore: DataStore<Preferences>,
    coroutineDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher
): LocationRepository =
    LocationRepository(
        deviceLocationLocalDataSource = DefaultDeviceLocationLocalDataSource(
            coroutineDispatcher = coroutineDispatcher,
            mainDispatcher = mainDispatcher
        ),
        dataStore = dataStore
    )