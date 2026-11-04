package se.vadretapp.vadret.pushalert.local

import androidx.datastore.core.DataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.KSerializer
import okio.Path
import okio.Path.Companion.toPath
import se.vadretapp.vadret.encryption.DeviceCipher
import se.vadretapp.vadret.encryption.model.KeyAlias

internal fun <T> encryptedDataStore(
    directoryPath: String,
    fileName: String,
    serializer: KSerializer<T>,
    defaultValue: T,
    ioDispatcher: CoroutineDispatcher
): DataStore<T> =
    encryptedDataStore(
        path = directoryPath.toPath() / fileName,
        deviceCipher = DeviceCipher(keyAlias = KeyAlias(value = KEY_ALIAS_PREFIX + fileName)),
        serializer = serializer,
        defaultValue = defaultValue,
        scope = CoroutineScope(context = ioDispatcher + SupervisorJob())
    )

internal expect fun <T> encryptedDataStore(
    path: Path,
    deviceCipher: DeviceCipher,
    serializer: KSerializer<T>,
    defaultValue: T,
    scope: CoroutineScope
): DataStore<T>

private const val KEY_ALIAS_PREFIX = "se.vadretapp.vadret.datastore."