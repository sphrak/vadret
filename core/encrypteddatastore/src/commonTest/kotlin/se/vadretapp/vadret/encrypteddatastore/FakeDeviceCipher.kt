package se.vadretapp.vadret.encrypteddatastore

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.encryption.DeviceCipher
import se.vadretapp.vadret.encryption.error.EncryptionError
import kotlin.experimental.xor

internal class FakeDeviceCipher(
    private val key: Byte,
    private val isUnavailable: Boolean = false
) : DeviceCipher {
    override fun encrypt(
        plaintext: ByteArray,
        associatedData: ByteArray
    ): Either<EncryptionError, ByteArray> {
        if (isUnavailable) {
            return EncryptionError.KEY_STORE_UNAVAILABLE.asLeft()
        }
        return (byteArrayOf(key, associatedData.size.toByte()) + associatedData + plaintext.scrambled()).asRight()
    }

    override fun decrypt(
        ciphertext: ByteArray,
        associatedData: ByteArray
    ): Either<EncryptionError, ByteArray> {
        val headerSize = HEADER_SIZE + associatedData.size
        return when {
            isUnavailable -> EncryptionError.KEY_STORE_UNAVAILABLE.asLeft()
            ciphertext.size < headerSize -> EncryptionError.NOT_FOR_THIS_KEY.asLeft()
            ciphertext[0] != key -> EncryptionError.NOT_FOR_THIS_KEY.asLeft()
            ciphertext[1].toInt() != associatedData.size -> EncryptionError.NOT_FOR_THIS_KEY.asLeft()
            !ciphertext.copyOfRange(fromIndex = HEADER_SIZE, toIndex = headerSize).contentEquals(other = associatedData) -> EncryptionError.NOT_FOR_THIS_KEY.asLeft()
            else -> ciphertext.copyOfRange(fromIndex = headerSize, toIndex = ciphertext.size).scrambled().asRight()
        }
    }

    private fun ByteArray.scrambled(): ByteArray = ByteArray(size = size) { index -> this[index] xor key }
}

private const val HEADER_SIZE = 2