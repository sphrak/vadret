package se.vadretapp.vadret.encryption

import io.github.sphrak.either.Either
import se.vadretapp.vadret.encryption.error.EncryptionError
import se.vadretapp.vadret.encryption.model.KeyAlias

interface DeviceCipher {
    fun encrypt(
        plaintext: ByteArray,
        associatedData: ByteArray
    ): Either<EncryptionError, ByteArray>

    fun decrypt(
        ciphertext: ByteArray,
        associatedData: ByteArray
    ): Either<EncryptionError, ByteArray>
}

expect fun DeviceCipher(keyAlias: KeyAlias): DeviceCipher