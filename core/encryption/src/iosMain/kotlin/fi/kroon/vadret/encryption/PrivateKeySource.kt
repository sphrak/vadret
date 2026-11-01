@file:OptIn(ExperimentalForeignApi::class)

package fi.kroon.vadret.encryption

import fi.kroon.vadret.encryption.error.EncryptionError
import io.github.sphrak.either.Either
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Security.SecKeyRef

internal fun interface PrivateKeySource {
    fun privateKey(): Either<EncryptionError, SecKeyRef>
}