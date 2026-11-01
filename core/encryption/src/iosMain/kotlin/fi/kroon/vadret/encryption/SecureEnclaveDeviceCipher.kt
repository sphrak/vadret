@file:OptIn(ExperimentalForeignApi::class)

package fi.kroon.vadret.encryption

import fi.kroon.vadret.encryption.error.EncryptionError
import fi.kroon.vadret.encryption.extension.describeAndRelease
import fi.kroon.vadret.encryption.extension.sha256
import fi.kroon.vadret.encryption.extension.toByteArray
import fi.kroon.vadret.encryption.extension.toCFData
import fi.kroon.vadret.encryption.model.KeyAlias
import fi.kroon.vadret.logging.Logger
import fi.kroon.vadret.logging.LoggerFactory
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFRelease
import platform.Security.SecKeyCopyPublicKey
import platform.Security.SecKeyCreateDecryptedData
import platform.Security.SecKeyCreateEncryptedData
import platform.Security.SecKeyRef
import platform.Security.kSecKeyAlgorithmECIESEncryptionCofactorVariableIVX963SHA256AESGCM

internal class SecureEnclaveDeviceCipher(
    private val keyAlias: KeyAlias,
    private val privateKeySource: PrivateKeySource
) : DeviceCipher {
    override fun encrypt(
        plaintext: ByteArray,
        associatedData: ByteArray
    ): Either<EncryptionError, ByteArray> =
        withPrivateKey { privateKey ->
            val publicKey = SecKeyCopyPublicKey(privateKey)
            if (publicKey == null) {
                unavailable(description = "the key has no public half")
            } else {
                val sealed = seal(
                    publicKey = publicKey,
                    plaintext = associatedData.sha256() + plaintext
                )
                CFRelease(publicKey)
                sealed
            }
        }

    override fun decrypt(
        ciphertext: ByteArray,
        associatedData: ByteArray
    ): Either<EncryptionError, ByteArray> =
        withPrivateKey { privateKey ->
            when (val opened = open(privateKey = privateKey, ciphertext = ciphertext)) {
                is Either.Left -> opened
                is Either.Right -> opened.b.withoutAssociatedData(associatedData = associatedData)
            }
        }

    private fun withPrivateKey(block: (SecKeyRef) -> Either<EncryptionError, ByteArray>): Either<EncryptionError, ByteArray> =
        when (val found = privateKeySource.privateKey()) {
            is Either.Left -> found
            is Either.Right -> {
                val result = block(found.b)
                CFRelease(found.b)
                result
            }
        }

    private fun seal(
        publicKey: SecKeyRef,
        plaintext: ByteArray
    ): Either<EncryptionError, ByteArray> = memScoped {
        val input = plaintext.toCFData()
        val error = alloc<CFErrorRefVar>()
        val sealed = SecKeyCreateEncryptedData(publicKey, kSecKeyAlgorithmECIESEncryptionCofactorVariableIVX963SHA256AESGCM, input, error.ptr)
        CFRelease(input)
        if (sealed == null) {
            unavailable(description = "sealing failed: ${error.value.describeAndRelease()}")
        } else {
            val bytes = sealed.toByteArray()
            CFRelease(sealed)
            bytes.asRight()
        }
    }

    private fun open(
        privateKey: SecKeyRef,
        ciphertext: ByteArray
    ): Either<EncryptionError, ByteArray> = memScoped {
        val input = ciphertext.toCFData()
        val error = alloc<CFErrorRefVar>()
        val opened = SecKeyCreateDecryptedData(privateKey, kSecKeyAlgorithmECIESEncryptionCofactorVariableIVX963SHA256AESGCM, input, error.ptr)
        CFRelease(input)
        if (opened == null) {
            notForThisKey(description = error.value.describeAndRelease())
        } else {
            val bytes = opened.toByteArray()
            CFRelease(opened)
            bytes.asRight()
        }
    }

    private fun ByteArray.withoutAssociatedData(associatedData: ByteArray): Either<EncryptionError, ByteArray> {
        val expected = associatedData.sha256()
        if (size < expected.size || !copyOfRange(fromIndex = 0, toIndex = expected.size).contentEquals(other = expected)) {
            return notForThisKey(description = "it was sealed for other associated data")
        }
        return copyOfRange(
            fromIndex = expected.size,
            toIndex = size
        ).asRight()
    }

    private fun unavailable(description: String): Either<EncryptionError, Nothing> {
        logger.warn { "${EncryptionError.KEY_STORE_UNAVAILABLE}: ${keyAlias.value}: $description" }
        return EncryptionError.KEY_STORE_UNAVAILABLE.asLeft()
    }

    private fun notForThisKey(description: String): Either<EncryptionError, Nothing> {
        logger.warn { "${EncryptionError.NOT_FOR_THIS_KEY}: ${keyAlias.value} cannot open this: $description" }
        return EncryptionError.NOT_FOR_THIS_KEY.asLeft()
    }
}

private val logger: Logger = LoggerFactory.getLogger(tag = "fi.kroon.vadret.encryption")