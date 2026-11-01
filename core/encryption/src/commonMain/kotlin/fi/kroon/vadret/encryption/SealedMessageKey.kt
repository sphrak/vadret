package fi.kroon.vadret.encryption

import fi.kroon.vadret.encryption.error.EncryptionError
import io.github.sphrak.either.Either

interface SealedMessageKey {
    fun publicKey(): Either<EncryptionError, ByteArray>

    fun open(sealed: ByteArray): Either<EncryptionError, ByteArray>
}