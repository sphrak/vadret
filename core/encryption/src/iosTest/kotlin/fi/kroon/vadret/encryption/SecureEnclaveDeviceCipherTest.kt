@file:OptIn(ExperimentalForeignApi::class)

package fi.kroon.vadret.encryption

import fi.kroon.vadret.encryption.error.EncryptionError
import fi.kroon.vadret.encryption.model.KeyAlias
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFNumberSInt32Type
import platform.Security.SecKeyCreateRandomKey
import platform.Security.SecKeyRef
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeECSECPrimeRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class SecureEnclaveDeviceCipherTest {
    private val deviceCipher = cipher(privateKeySource = InMemoryPrivateKeySource())

    @Test
    fun `what it encrypts it decrypts`() {
        val ciphertext = deviceCipher.encrypt(
            plaintext = PLAINTEXT,
            associatedData = ASSOCIATED_DATA
        ).rightOrEmpty()

        assertEquals(
            expected = PLAINTEXT.toList(),
            actual = deviceCipher.decrypt(
                ciphertext = ciphertext,
                associatedData = ASSOCIATED_DATA
            ).rightOrEmpty().toList()
        )
    }

    @Test
    fun `each ciphertext is new and hides the plaintext`() {
        val first = deviceCipher.encrypt(
            plaintext = PLAINTEXT,
            associatedData = ASSOCIATED_DATA
        ).rightOrEmpty()
        val second = deviceCipher.encrypt(
            plaintext = PLAINTEXT,
            associatedData = ASSOCIATED_DATA
        ).rightOrEmpty()

        assertNotEquals(
            illegal = first.toList(),
            actual = second.toList()
        )
        assertFalse(actual = first.decodeToString().contains(other = PLAINTEXT.decodeToString()))
    }

    @Test
    fun `empty plaintext and associated data still round trip`() {
        val ciphertext = deviceCipher.encrypt(
            plaintext = ByteArray(size = 0),
            associatedData = ByteArray(size = 0)
        ).rightOrEmpty()

        assertEquals(
            expected = emptyList(),
            actual = deviceCipher.decrypt(
                ciphertext = ciphertext,
                associatedData = ByteArray(size = 0)
            ).rightOrEmpty().toList()
        )
    }

    @Test
    fun `a ciphertext for other associated data is not for this key`() {
        val ciphertext = deviceCipher.encrypt(
            plaintext = PLAINTEXT,
            associatedData = ASSOCIATED_DATA
        ).rightOrEmpty()

        assertEquals(
            expected = EncryptionError.NOT_FOR_THIS_KEY.asLeft(),
            actual = deviceCipher.decrypt(
                ciphertext = ciphertext,
                associatedData = "settings".encodeToByteArray()
            )
        )
    }

    @Test
    fun `another key cannot open it`() {
        val ciphertext = deviceCipher.encrypt(
            plaintext = PLAINTEXT,
            associatedData = ASSOCIATED_DATA
        ).rightOrEmpty()

        assertEquals(
            expected = EncryptionError.NOT_FOR_THIS_KEY.asLeft(),
            actual = cipher(privateKeySource = InMemoryPrivateKeySource()).decrypt(
                ciphertext = ciphertext,
                associatedData = ASSOCIATED_DATA
            )
        )
    }

    @Test
    fun `bytes that are no ciphertext are not for this key`() {
        assertEquals(
            expected = EncryptionError.NOT_FOR_THIS_KEY.asLeft(),
            actual = deviceCipher.decrypt(
                ciphertext = "not sealed".encodeToByteArray(),
                associatedData = ASSOCIATED_DATA
            )
        )
    }

    @Test
    fun `a keychain that cannot hand out the key is unavailable`() {
        val unavailableCipher = cipher(privateKeySource = { EncryptionError.KEY_STORE_UNAVAILABLE.asLeft() })

        assertEquals(
            expected = List(size = 2) { EncryptionError.KEY_STORE_UNAVAILABLE.asLeft() },
            actual = listOf(
                unavailableCipher.encrypt(
                    plaintext = PLAINTEXT,
                    associatedData = ASSOCIATED_DATA
                ),
                unavailableCipher.decrypt(
                    ciphertext = PLAINTEXT,
                    associatedData = ASSOCIATED_DATA
                )
            )
        )
    }
}

private class InMemoryPrivateKeySource : PrivateKeySource {
    private val privateKey: SecKeyRef = memScoped {
        val keyBits = alloc<IntVar>()
        keyBits.value = P256_KEY_BITS
        val keySize = CFNumberCreate(kCFAllocatorDefault, kCFNumberSInt32Type, keyBits.ptr)
        val attributes = cfDictionaryOf(
            entries = listOf<Pair<CFStringRef?, CFTypeRef?>>(
                kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
                kSecAttrKeySizeInBits to keySize
            )
        )
        val key = checkNotNull(value = SecKeyCreateRandomKey(attributes, null)) { "No P-256 key could be made" }
        CFRelease(attributes)
        CFRelease(keySize)
        key
    }

    override fun privateKey(): Either<EncryptionError, SecKeyRef> {
        CFRetain(privateKey)
        return privateKey.asRight()
    }
}

private fun cipher(privateKeySource: PrivateKeySource): DeviceCipher =
    SecureEnclaveDeviceCipher(
        keyAlias = KeyAlias(value = "fi.kroon.vadret.test"),
        privateKeySource = privateKeySource
    )

private fun Either<EncryptionError, ByteArray>.rightOrEmpty(): ByteArray =
    when (this) {
        is Either.Left -> ByteArray(size = 0)
        is Either.Right -> b
    }

private val PLAINTEXT = """{"installation_token":"vadret_secret"}""".encodeToByteArray()
private val ASSOCIATED_DATA = "installation".encodeToByteArray()
private const val P256_KEY_BITS = 256