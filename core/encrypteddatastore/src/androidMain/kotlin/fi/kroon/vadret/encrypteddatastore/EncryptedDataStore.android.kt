package fi.kroon.vadret.encrypteddatastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.tink.AeadSerializer
import fi.kroon.vadret.encryption.DeviceCipher
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import okio.Path

internal actual fun <T> encryptedDataStore(
    path: Path,
    deviceCipher: DeviceCipher,
    serializer: KSerializer<T>,
    defaultValue: T,
    scope: CoroutineScope
): DataStore<T> =
    DataStoreFactory.create(
        serializer = AeadSerializer(
            aead = DeviceCipherAead(
                deviceCipher = deviceCipher,
                fileName = path.name
            ),
            wrappedSerializer = JsonSerializer(
                serializer = serializer,
                defaultValue = defaultValue,
                fileName = path.name
            ),
            associatedData = path.name.encodeToByteArray()
        ),
        corruptionHandler = corruptionHandler(
            fileName = path.name,
            defaultValue = defaultValue
        ),
        scope = scope,
        produceFile = { path.toFile() }
    )