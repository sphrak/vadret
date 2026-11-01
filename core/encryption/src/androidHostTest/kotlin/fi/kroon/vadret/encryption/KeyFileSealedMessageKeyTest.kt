package fi.kroon.vadret.encryption

import com.google.crypto.tink.subtle.AesGcmJce
import fi.kroon.vadret.encryption.error.EncryptionError
import fi.kroon.vadret.encryption.model.KeyAlias
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import java.io.File
import java.nio.file.Files
import kotlin.io.encoding.Base64
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class KeyFileSealedMessageKeyTest {
    private val directory: File = Files.createTempDirectory("sealed-message-key").toFile()
    private val keyFile = File(directory, "sealed_message_key")
    private val deviceCipher: DeviceCipher = AeadDeviceCipher(
        keyAlias = KeyAlias(value = "fi.kroon.vadret.test"),
        produceAead = { AesGcmJce(DEVICE_KEY) }
    )

    @AfterTest
    fun deleteDirectory() {
        directory.deleteRecursively()
    }

    @Test
    fun `a message sealed by the server opens with the key it was sealed for`() {
        writeServerKey()

        assertEquals(
            expected = PLAINTEXT,
            actual = sealedMessageKey().open(sealed = SEALED).rightOrNull()?.decodeToString()
        )
    }

    @Test
    fun `the public key is the uncompressed point the server seals for`() {
        writeServerKey()

        assertEquals(
            expected = PUBLIC_POINT.toList(),
            actual = sealedMessageKey().publicKey().rightOrNull()?.toList()
        )
    }

    @Test
    fun `a new key is made once and kept`() {
        val first = sealedMessageKey().publicKey().rightOrNull()
        val second = sealedMessageKey().publicKey().rightOrNull()

        assertEquals(
            expected = listOf(UNCOMPRESSED_POINT_SIZE, UNCOMPRESSED_POINT_PREFIX.toInt()),
            actual = listOf(first?.size, first?.first()?.toInt())
        )
        assertEquals(
            expected = first?.toList(),
            actual = second?.toList()
        )
    }

    @Test
    fun `another key cannot open the message`() {
        sealedMessageKey().publicKey()

        assertEquals(
            expected = EncryptionError.NOT_FOR_THIS_KEY.asLeft(),
            actual = sealedMessageKey().open(sealed = SEALED)
        )
    }

    @Test
    fun `a tampered, cut short or off curve message is not for this key`() {
        writeServerKey()
        val tampered = SEALED.copyOf()
        tampered[tampered.lastIndex] = (tampered.last() + 1).toByte()
        val offCurve = SEALED.copyOf()
        offCurve[1] = (offCurve[1] + 1).toByte()

        assertEquals(
            expected = List(size = 3) { EncryptionError.NOT_FOR_THIS_KEY.asLeft() },
            actual = listOf(tampered, SEALED.copyOfRange(fromIndex = 0, toIndex = 70), offCurve).map { sealed -> sealedMessageKey().open(sealed = sealed) }
        )
    }

    @Test
    fun `a key file the device key no longer opens is replaced by a new key`() {
        writeServerKey()
        val otherDeviceCipher: DeviceCipher = AeadDeviceCipher(
            keyAlias = KeyAlias(value = "fi.kroon.vadret.test"),
            produceAead = { AesGcmJce(OTHER_DEVICE_KEY) }
        )

        val replacement = SealedMessageKey(
            keyFile = keyFile,
            deviceCipher = otherDeviceCipher
        ).publicKey().rightOrNull()

        assertEquals(
            expected = UNCOMPRESSED_POINT_SIZE,
            actual = replacement?.size
        )
        assertNotEquals(
            illegal = PUBLIC_POINT.toList(),
            actual = replacement?.toList()
        )
    }

    @Test
    fun `opening a push on a device without a key makes no key`() {
        assertEquals(
            expected = EncryptionError.NOT_FOR_THIS_KEY.asLeft(),
            actual = sealedMessageKey().open(sealed = SEALED)
        )
        assertEquals(
            expected = false,
            actual = keyFile.exists()
        )
    }

    private fun sealedMessageKey(): SealedMessageKey = SealedMessageKey(
        keyFile = keyFile,
        deviceCipher = deviceCipher
    )

    private fun writeServerKey() {
        val sealedKeyPair = deviceCipher.encrypt(
            plaintext = PUBLIC_POINT + PKCS8,
            associatedData = "fi.kroon.vadret.sealedmessagekey".encodeToByteArray()
        ).rightOrNull()
        keyFile.writeBytes(checkNotNull(value = sealedKeyPair))
    }
}

private fun <T> Either<EncryptionError, T>.rightOrNull(): T? = when (this) {
    is Either.Left -> null
    is Either.Right -> b
}

private val DEVICE_KEY = ByteArray(size = 32) { index -> index.toByte() }
private val OTHER_DEVICE_KEY = ByteArray(size = 32) { index -> (index + 1).toByte() }
private val PKCS8 = Base64.decode(source = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCAPBHE2QMIEJRH4X0ArEUw7bIVKXS+USXEDWe5kGR7XqA==")
private val PUBLIC_POINT = Base64.decode(source = "BGJ0AnQQ0CUq9hUYqDeemnaWjR3FOgDQ/hyTKoCRyPqPBkYcAzinUYMMknvB8JXepWnVCp7Gta/q8tFzOK+8l7I=")
private val SEALED = Base64.decode(source = "BEMSv8COK/25apL9e87hhBcXx66VlKiscNL99uErOTiF2IRx/1DQumVsMFWAabHkPx1Y4gtZsPIZqzK6wxmc0Sw7ODxlo8Fb0A0uFa0hQBOHT5jIiKKMMEUeXczRgbTOrwpCKX121A2ywEcku0kHi+UnRtNeIznsozTLwK9vknrBNTq8UbexGhuBv+EiRj1DT9VYt4uBrufZ5o3Ws40u0QtxODmyitWoMYMSHPdMdMlIrItscpcqGiPgQg==")
private const val PLAINTEXT = "{\"title\":\"Gul varning: Stockholms län\",\"body\":\"Kuling\",\"data\":{\"kind\":\"warning\",\"notification_id\":\"n-1\"}}"
private const val UNCOMPRESSED_POINT_SIZE = 65
private const val UNCOMPRESSED_POINT_PREFIX: Byte = 0x04