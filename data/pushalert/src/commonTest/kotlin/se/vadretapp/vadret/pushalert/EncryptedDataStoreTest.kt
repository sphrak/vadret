package se.vadretapp.vadret.pushalert

import androidx.datastore.core.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.IOException
import okio.Path
import se.vadretapp.vadret.encryption.DeviceCipher
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.pushalert.local.encryptedDataStore
import se.vadretapp.vadret.testing.deleteRecursively
import se.vadretapp.vadret.testing.temporaryDirectory
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class EncryptedDataStoreTest {
    private val directory: Path = temporaryDirectory(prefix = "encrypted-data-store")
    private val path: Path = directory / FILE_NAME

    @AfterTest
    fun deleteDirectory() {
        directory.deleteRecursively()
    }

    @Test
    fun `a store with no file yet holds the default value`() = runTest {
        assertEquals(
            expected = NO_INSTALLATION,
            actual = readOnce(deviceCipher = FakeDeviceCipher(key = KEY))
        )
    }

    @Test
    fun `a saved value is read back by the next store on the file`() = runTest {
        saveOnce(deviceCipher = FakeDeviceCipher(key = KEY))

        assertEquals(
            expected = INSTALLATION,
            actual = readOnce(deviceCipher = FakeDeviceCipher(key = KEY))
        )
    }

    @Test
    fun `the file on disk holds only ciphertext`() = runTest {
        saveOnce(deviceCipher = FakeDeviceCipher(key = KEY))

        val stored = FileSystem.SYSTEM.read(file = path) { readByteArray() }

        assertFalse(actual = stored.decodeToString().contains(other = TOKEN))
    }

    @Test
    fun `a file another key wrote starts over at the default and says so`() = runTest {
        saveOnce(deviceCipher = FakeDeviceCipher(key = KEY))

        withRecordedLogs { recordingLogSink ->
            assertEquals(
                expected = NO_INSTALLATION,
                actual = readOnce(deviceCipher = FakeDeviceCipher(key = OTHER_KEY))
            )
            assertEquals(
                expected = listOf(LogLevel.WARN),
                actual = recordingLogSink.lines.map { line -> line.level }
            )
        }
    }

    @Test
    fun `a file copied in under another name starts over`() = runTest {
        saveOnce(deviceCipher = FakeDeviceCipher(key = KEY))
        val otherPath = directory / OTHER_FILE_NAME
        FileSystem.SYSTEM.copy(
            source = path,
            target = otherPath
        )

        assertEquals(
            expected = NO_INSTALLATION,
            actual = readOnce(
                deviceCipher = FakeDeviceCipher(key = KEY),
                storePath = otherPath
            )
        )
    }

    @Test
    fun `a key that cannot be used right now fails the read and leaves the file alone`() = runTest {
        saveOnce(deviceCipher = FakeDeviceCipher(key = KEY))
        val stored = FileSystem.SYSTEM.read(file = path) { readByteArray() }

        assertFailsWith<IOException> {
            readOnce(
                deviceCipher = FakeDeviceCipher(
                    key = KEY,
                    isUnavailable = true
                )
            )
        }
        assertEquals(
            expected = stored.toList(),
            actual = FileSystem.SYSTEM.read(file = path) { readByteArray() }.toList()
        )
    }

    @Test
    fun `a key that cannot be used right now fails the write`() = runTest {
        val scope = storeScope()
        val dataStore = store(
            deviceCipher = FakeDeviceCipher(
                key = KEY,
                isUnavailable = true
            ),
            scope = scope
        )

        assertFailsWith<IOException> { dataStore.updateData { INSTALLATION } }
        scope.coroutineContext.job.cancelAndJoin()
    }

    private suspend fun saveOnce(deviceCipher: DeviceCipher) {
        val scope = storeScope()
        store(
            deviceCipher = deviceCipher,
            scope = scope
        ).updateData { INSTALLATION }
        scope.coroutineContext.job.cancelAndJoin()
    }

    private suspend fun readOnce(
        deviceCipher: DeviceCipher,
        storePath: Path = path
    ): TestInstallation {
        val scope = storeScope()
        try {
            return store(
                deviceCipher = deviceCipher,
                scope = scope,
                storePath = storePath
            ).data.first()
        } finally {
            scope.coroutineContext.job.cancelAndJoin()
        }
    }

    private fun store(
        deviceCipher: DeviceCipher,
        scope: CoroutineScope,
        storePath: Path = path
    ): DataStore<TestInstallation> =
        encryptedDataStore(
            path = storePath,
            deviceCipher = deviceCipher,
            serializer = TestInstallation.serializer(),
            defaultValue = NO_INSTALLATION,
            scope = scope
        )
}

@Serializable
internal data class TestInstallation(
    @SerialName(value = "installation_token")
    val installationToken: String? = null
)

private fun storeScope(): CoroutineScope = CoroutineScope(context = Dispatchers.Default + SupervisorJob())

private const val FILE_NAME = "installation.json"
private const val OTHER_FILE_NAME = "settings.json"
private const val TOKEN = "vadret_secret"
private const val KEY: Byte = 7
private const val OTHER_KEY: Byte = 9
private val INSTALLATION = TestInstallation(installationToken = TOKEN)
private val NO_INSTALLATION = TestInstallation()