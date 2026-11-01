package se.vadretapp.vadret.encrypteddatastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import okio.FileSystem
import okio.Path
import se.vadretapp.vadret.encryption.DeviceCipher

internal actual fun <T> encryptedDataStore(
    path: Path,
    deviceCipher: DeviceCipher,
    serializer: KSerializer<T>,
    defaultValue: T,
    scope: CoroutineScope
): DataStore<T> =
    DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = DeviceCipherSerializer(
                serializer = serializer,
                defaultValue = defaultValue,
                deviceCipher = deviceCipher,
                fileName = path.name
            ),
            producePath = { path }
        ),
        corruptionHandler = corruptionHandler(
            fileName = path.name,
            defaultValue = defaultValue
        ),
        scope = scope
    )