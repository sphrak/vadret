@file:OptIn(ExperimentalForeignApi::class)

package se.vadretapp.vadret.encryption

import io.github.sphrak.either.Either
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Security.SecKeyRef
import se.vadretapp.vadret.encryption.error.EncryptionError

internal fun interface PrivateKeySource {
    fun privateKey(): Either<EncryptionError, SecKeyRef>
}