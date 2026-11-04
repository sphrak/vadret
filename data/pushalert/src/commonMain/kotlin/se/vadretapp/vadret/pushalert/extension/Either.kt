package se.vadretapp.vadret.pushalert.extension

import androidx.datastore.core.CorruptionException
import io.github.sphrak.either.Either
import okio.IOException
import se.vadretapp.vadret.encryption.error.EncryptionError

internal fun Either<EncryptionError, ByteArray>.orThrow(fileName: String): ByteArray =
    when (this) {
        is Either.Right -> b
        is Either.Left -> when (a) {
            EncryptionError.NOT_FOR_THIS_KEY -> throw CorruptionException(message = "$fileName was not encrypted with this phone's key")
            EncryptionError.KEY_STORE_UNAVAILABLE -> throw IOException("The key for $fileName cannot be used right now")
        }
    }