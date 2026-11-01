package se.vadretapp.vadret.encryption

import io.github.sphrak.either.Either
import se.vadretapp.vadret.encryption.error.EncryptionError

interface SealedMessageKey {
    fun publicKey(): Either<EncryptionError, ByteArray>

    fun open(sealed: ByteArray): Either<EncryptionError, ByteArray>
}