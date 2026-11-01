package se.vadretapp.vadret.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import okio.Path

private const val DIRECTORY_PREFIX = "datastore-test"
private const val PREFERENCES_FILE_NAME = "test.preferences_pb"

fun testPreferencesDataStore(scope: CoroutineScope): DataStore<Preferences> {
    val file: Path = temporaryDirectory(prefix = DIRECTORY_PREFIX) / PREFERENCES_FILE_NAME
    return PreferenceDataStoreFactory.createWithPath(
        scope = scope,
        produceFile = { file }
    )
}