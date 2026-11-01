package fi.kroon.vadret.encryption

import fi.kroon.vadret.encryption.error.EncryptionError
import fi.kroon.vadret.encryption.model.KeyAlias
import io.github.sphrak.either.Either

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