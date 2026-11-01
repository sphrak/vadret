@file:OptIn(ExperimentalForeignApi::class)

package se.vadretapp.vadret.encryption

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
import platform.Security.SecKeyCopyExternalRepresentation
import platform.Security.SecKeyCopyPublicKey
import platform.Security.SecKeyCreateDecryptedData
import platform.Security.SecKeyRef
import platform.Security.kSecKeyAlgorithmECIESEncryptionCofactorVariableIVX963SHA256AESGCM
import se.vadretapp.vadret.encryption.error.EncryptionError
import se.vadretapp.vadret.encryption.extension.describeAndRelease
import se.vadretapp.vadret.encryption.extension.toByteArray
import se.vadretapp.vadret.encryption.extension.toCFData
import se.vadretapp.vadret.encryption.model.KeyAlias
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory

internal class SecureEnclaveSealedMessageKey(
    private val keyAlias: KeyAlias,
    private val privateKeySource: PrivateKeySource
) : SealedMessageKey {
    override fun publicKey(): Either<EncryptionError, ByteArray> =
        withPrivateKey { privateKey ->
            val publicKey = SecKeyCopyPublicKey(privateKey)
            if (publicKey == null) {
                unavailable(description = "the key has no public half")
            } else {
                val publicPoint = externalRepresentation(publicKey = publicKey)
                CFRelease(publicKey)
                publicPoint
            }
        }

    override fun open(sealed: ByteArray): Either<EncryptionError, ByteArray> =
        withPrivateKey { privateKey ->
            memScoped {
                val input = sealed.toCFData()
                val error = alloc<CFErrorRefVar>()
                val opened = SecKeyCreateDecryptedData(privateKey, kSecKeyAlgorithmECIESEncryptionCofactorVariableIVX963SHA256AESGCM, input, error.ptr)
                CFRelease(input)
                if (opened == null) {
                    notForThisKey(description = error.value.describeAndRelease())
                } else {
                    val plaintext = opened.toByteArray()
                    CFRelease(opened)
                    plaintext.asRight()
                }
            }
        }

    private fun externalRepresentation(publicKey: SecKeyRef): Either<EncryptionError, ByteArray> = memScoped {
        val error = alloc<CFErrorRefVar>()
        val representation = SecKeyCopyExternalRepresentation(publicKey, error.ptr)
        if (representation == null) {
            unavailable(description = "the public key cannot be exported: ${error.value.describeAndRelease()}")
        } else {
            val publicPoint = representation.toByteArray()
            CFRelease(representation)
            publicPoint.asRight()
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

    private fun unavailable(description: String): Either<EncryptionError, Nothing> {
        logger.warn { "${EncryptionError.KEY_STORE_UNAVAILABLE}: ${keyAlias.value}: $description" }
        return EncryptionError.KEY_STORE_UNAVAILABLE.asLeft()
    }

    private fun notForThisKey(description: String): Either<EncryptionError, Nothing> {
        logger.warn { "${EncryptionError.NOT_FOR_THIS_KEY}: ${keyAlias.value} cannot open this sealed message: $description" }
        return EncryptionError.NOT_FOR_THIS_KEY.asLeft()
    }
}

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.encryption")