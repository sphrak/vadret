package se.vadretapp.vadret.encryption

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.encryption.error.EncryptionError
import se.vadretapp.vadret.encryption.extension.toP256PublicKeyOrNull
import se.vadretapp.vadret.encryption.extension.toUncompressedPoint
import se.vadretapp.vadret.encryption.model.SealedMessageKeyPair
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class KeyFileSealedMessageKey(
    private val keyFile: File,
    private val deviceCipher: DeviceCipher
) : SealedMessageKey {
    private val keyFileLock = Any()

    override fun publicKey(): Either<EncryptionError, ByteArray> =
        when (val keyPair = keyPair()) {
            is Either.Left -> keyPair
            is Either.Right -> keyPair.b.publicPoint.asRight()
        }

    override fun open(sealed: ByteArray): Either<EncryptionError, ByteArray> {
        if (sealed.size < UNCOMPRESSED_POINT_BYTES + GCM_TAG_BYTES) {
            return notForThisKey(description = "it is too short to be sealed")
        }
        val ephemeralPoint = sealed.copyOfRange(fromIndex = 0, toIndex = UNCOMPRESSED_POINT_BYTES)
        val ephemeralPublicKey = ephemeralPoint.toP256PublicKeyOrNull() ?: return notForThisKey(description = "its ephemeral key is not a point on P-256")
        return when (val keyPair = existingKeyPair()) {
            is Either.Left -> keyPair
            is Either.Right -> decrypt(
                privateKey = keyPair.b.privateKey,
                ephemeralPublicKey = ephemeralPublicKey,
                ephemeralPoint = ephemeralPoint,
                ciphertext = sealed.copyOfRange(fromIndex = UNCOMPRESSED_POINT_BYTES, toIndex = sealed.size)
            )
        }
    }

    private fun decrypt(
        privateKey: PrivateKey,
        ephemeralPublicKey: ECPublicKey,
        ephemeralPoint: ByteArray,
        ciphertext: ByteArray
    ): Either<EncryptionError, ByteArray> =
        try {
            val keyAgreement = KeyAgreement.getInstance(ECDH_ALGORITHM)
            keyAgreement.init(privateKey)
            keyAgreement.doPhase(ephemeralPublicKey, true)
            val keyAndIv = x963Sha256(
                sharedSecret = keyAgreement.generateSecret(),
                sharedInfo = ephemeralPoint
            )
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyAndIv, 0, AES_KEY_BYTES, AES_ALGORITHM),
                GCMParameterSpec(GCM_TAG_BITS, keyAndIv, AES_KEY_BYTES, GCM_IV_BYTES)
            )
            cipher.doFinal(ciphertext).asRight()
        } catch (exception: AEADBadTagException) {
            notForThisKey(description = "its tag does not match: ${exception.message}")
        } catch (exception: GeneralSecurityException) {
            unavailable(description = "it could not be opened: ${exception.message}")
        }

    private fun keyPair(): Either<EncryptionError, SealedMessageKeyPair> =
        synchronized(lock = keyFileLock) {
            if (!keyFile.exists()) {
                createKeyPair()
            } else {
                when (val keyPair = readKeyPair()) {
                    is Either.Left -> if (keyPair.a == EncryptionError.NOT_FOR_THIS_KEY) {
                        logger.warn { "${EncryptionError.NOT_FOR_THIS_KEY}: ${keyFile.name} no longer opens with this device's key, so a new key replaces it" }
                        createKeyPair()
                    } else {
                        keyPair
                    }
                    is Either.Right -> keyPair
                }
            }
        }

    private fun existingKeyPair(): Either<EncryptionError, SealedMessageKeyPair> =
        synchronized(lock = keyFileLock) {
            if (keyFile.exists()) {
                readKeyPair()
            } else {
                notForThisKey(description = "this device has no key yet")
            }
        }

    private fun readKeyPair(): Either<EncryptionError, SealedMessageKeyPair> {
        val sealedKeyPair = try {
            keyFile.readBytes()
        } catch (exception: IOException) {
            return unavailable(description = "${keyFile.name} could not be read: ${exception.message}")
        }
        return when (val keyPair = deviceCipher.decrypt(ciphertext = sealedKeyPair, associatedData = KEY_FILE_ASSOCIATED_DATA)) {
            is Either.Left -> keyPair
            is Either.Right -> keyPair.b.toSealedMessageKeyPair()
        }
    }

    private fun createKeyPair(): Either<EncryptionError, SealedMessageKeyPair> {
        val keyPairGenerator = KeyPairGenerator.getInstance(EC_ALGORITHM)
        keyPairGenerator.initialize(ECGenParameterSpec(P256_CURVE))
        val keyPair = keyPairGenerator.generateKeyPair()
        val publicPoint = (keyPair.public as ECPublicKey).toUncompressedPoint()
        val sealedKeyPair = when (val encrypted = deviceCipher.encrypt(plaintext = publicPoint + keyPair.private.encoded, associatedData = KEY_FILE_ASSOCIATED_DATA)) {
            is Either.Left -> return encrypted
            is Either.Right -> encrypted.b
        }
        try {
            keyFile.parentFile?.mkdirs()
            val temporaryFile = File(keyFile.parentFile, keyFile.name + TEMPORARY_SUFFIX)
            temporaryFile.writeBytes(sealedKeyPair)
            if (!temporaryFile.renameTo(keyFile)) {
                return unavailable(description = "${keyFile.name} could not be put in place")
            }
        } catch (exception: IOException) {
            return unavailable(description = "${keyFile.name} could not be written: ${exception.message}")
        }
        return SealedMessageKeyPair(
            publicPoint = publicPoint,
            privateKey = keyPair.private
        ).asRight()
    }

    private fun ByteArray.toSealedMessageKeyPair(): Either<EncryptionError, SealedMessageKeyPair> =
        try {
            SealedMessageKeyPair(
                publicPoint = copyOfRange(fromIndex = 0, toIndex = UNCOMPRESSED_POINT_BYTES),
                privateKey = KeyFactory.getInstance(EC_ALGORITHM).generatePrivate(PKCS8EncodedKeySpec(copyOfRange(fromIndex = UNCOMPRESSED_POINT_BYTES, toIndex = size)))
            ).asRight()
        } catch (exception: GeneralSecurityException) {
            notForThisKey(description = "${keyFile.name} holds no key: ${exception.message}")
        } catch (exception: IndexOutOfBoundsException) {
            notForThisKey(description = "${keyFile.name} is too short to hold a key: ${exception.message}")
        }

    private fun notForThisKey(description: String): Either<EncryptionError, Nothing> {
        logger.warn { "${EncryptionError.NOT_FOR_THIS_KEY}: a sealed message cannot be opened, $description" }
        return EncryptionError.NOT_FOR_THIS_KEY.asLeft()
    }

    private fun unavailable(description: String): Either<EncryptionError, Nothing> {
        logger.warn { "${EncryptionError.KEY_STORE_UNAVAILABLE}: $description" }
        return EncryptionError.KEY_STORE_UNAVAILABLE.asLeft()
    }
}

private fun x963Sha256(
    sharedSecret: ByteArray,
    sharedInfo: ByteArray
): ByteArray {
    val messageDigest = MessageDigest.getInstance(SHA_256_ALGORITHM)
    messageDigest.update(sharedSecret)
    messageDigest.update(FIRST_COUNTER)
    messageDigest.update(sharedInfo)
    return messageDigest.digest()
}

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.encryption")

private val KEY_FILE_ASSOCIATED_DATA = "se.vadretapp.vadret.sealedmessagekey".encodeToByteArray()
private val FIRST_COUNTER = byteArrayOf(0, 0, 0, 1)

private const val ECDH_ALGORITHM = "ECDH"
private const val SHA_256_ALGORITHM = "SHA-256"
private const val AES_ALGORITHM = "AES"
private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
private const val AES_KEY_BYTES = 16
private const val GCM_IV_BYTES = 16
private const val GCM_TAG_BITS = 128
private const val GCM_TAG_BYTES = 16
private const val TEMPORARY_SUFFIX = ".new"