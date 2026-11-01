package se.vadretapp.vadret.encryption

import com.google.crypto.tink.Aead
import com.google.crypto.tink.subtle.AesGcmJce
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import se.vadretapp.vadret.encryption.error.EncryptionError
import se.vadretapp.vadret.encryption.model.KeyAlias
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.testing.withRecordedLogs
import java.security.GeneralSecurityException
import java.security.ProviderException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AeadDeviceCipherTest {
    @Test
    fun `what it encrypts it decrypts`() {
        val deviceCipher = deviceCipher(aead = AesGcmJce(KEY))

        val ciphertext = deviceCipher.encrypt(
            plaintext = PLAINTEXT,
            associatedData = ASSOCIATED_DATA
        ).rightOrEmpty()

        assertFalse(actual = ciphertext.decodeToString().contains(other = PLAINTEXT.decodeToString()))
        assertEquals(
            expected = PLAINTEXT.toList(),
            actual = deviceCipher.decrypt(
                ciphertext = ciphertext,
                associatedData = ASSOCIATED_DATA
            ).rightOrEmpty().toList()
        )
    }

    @Test
    fun `a ciphertext for other associated data is not for this key`() {
        val deviceCipher = deviceCipher(aead = AesGcmJce(KEY))
        val ciphertext = deviceCipher.encrypt(
            plaintext = PLAINTEXT,
            associatedData = ASSOCIATED_DATA
        ).rightOrEmpty()

        withRecordedLogs { recordingLogSink ->
            assertEquals(
                expected = EncryptionError.NOT_FOR_THIS_KEY.asLeft(),
                actual = deviceCipher.decrypt(
                    ciphertext = ciphertext,
                    associatedData = "settings".encodeToByteArray()
                )
            )
            assertEquals(
                expected = listOf(LogLevel.WARN),
                actual = recordingLogSink.lines.map { line -> line.level }
            )
        }
    }

    @Test
    fun `a ciphertext made with another key is not for this key`() {
        val ciphertext = deviceCipher(aead = AesGcmJce(KEY)).encrypt(
            plaintext = PLAINTEXT,
            associatedData = ASSOCIATED_DATA
        ).rightOrEmpty()

        val decrypted = deviceCipher(aead = AesGcmJce(OTHER_KEY)).decrypt(
            ciphertext = ciphertext,
            associatedData = ASSOCIATED_DATA
        )

        assertEquals(
            expected = EncryptionError.NOT_FOR_THIS_KEY.asLeft(),
            actual = decrypted
        )
    }

    @Test
    fun `a keystore that fails for a moment is unavailable rather than wrong`() {
        val failures = listOf(GeneralSecurityException("busy"), ProviderException("keystore died"))

        val results = failures.flatMap { failure ->
            val deviceCipher = deviceCipher(aead = FailingAead(failure = failure))
            listOf(
                deviceCipher.encrypt(
                    plaintext = PLAINTEXT,
                    associatedData = ASSOCIATED_DATA
                ),
                deviceCipher.decrypt(
                    ciphertext = PLAINTEXT,
                    associatedData = ASSOCIATED_DATA
                )
            )
        }

        assertEquals(
            expected = List(size = results.size) { EncryptionError.KEY_STORE_UNAVAILABLE.asLeft() },
            actual = results
        )
    }

    @Test
    fun `the key is fetched once, on first use`() {
        var fetches = 0
        val deviceCipher = AeadDeviceCipher(
            keyAlias = KEY_ALIAS,
            produceAead = {
                fetches += 1
                AesGcmJce(KEY)
            }
        )
        val fetchesBeforeUse = fetches

        repeat(times = 3) {
            deviceCipher.encrypt(
                plaintext = PLAINTEXT,
                associatedData = ASSOCIATED_DATA
            )
        }

        assertEquals(
            expected = listOf(0, 1),
            actual = listOf(fetchesBeforeUse, fetches)
        )
    }
}

private class FailingAead(private val failure: Exception) : Aead {
    override fun encrypt(
        plaintext: ByteArray,
        associatedData: ByteArray
    ): ByteArray = throw failure

    override fun decrypt(
        ciphertext: ByteArray,
        associatedData: ByteArray
    ): ByteArray = throw failure
}

private fun deviceCipher(aead: Aead): DeviceCipher =
    AeadDeviceCipher(
        keyAlias = KEY_ALIAS,
        produceAead = { aead }
    )

private fun Either<EncryptionError, ByteArray>.rightOrEmpty(): ByteArray =
    when (this) {
        is Either.Left -> ByteArray(size = 0)
        is Either.Right -> b
    }

private val KEY_ALIAS = KeyAlias(value = "se.vadretapp.vadret.test")
private val KEY = ByteArray(size = 32) { index -> index.toByte() }
private val OTHER_KEY = ByteArray(size = 32) { index -> (index + 1).toByte() }
private val PLAINTEXT = """{"installation_token":"vadret_secret"}""".encodeToByteArray()
private val ASSOCIATED_DATA = "installation".encodeToByteArray()