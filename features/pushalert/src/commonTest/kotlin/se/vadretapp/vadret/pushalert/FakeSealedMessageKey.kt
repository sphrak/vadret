package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.encryption.SealedMessageKey
import se.vadretapp.vadret.encryption.error.EncryptionError

internal class FakeSealedMessageKey(
    private val openedBySealed: Map<List<Byte>, ByteArray> = emptyMap(),
    private val isUnavailable: Boolean = false
) : SealedMessageKey {
    override fun publicKey(): Either<EncryptionError, ByteArray> = if (isUnavailable) {
        EncryptionError.KEY_STORE_UNAVAILABLE.asLeft()
    } else {
        ByteArray(size = PUBLIC_POINT_SIZE) { 4 }.asRight()
    }

    override fun open(sealed: ByteArray): Either<EncryptionError, ByteArray> =
        openedBySealed[sealed.toList()]?.asRight() ?: EncryptionError.NOT_FOR_THIS_KEY.asLeft()
}

private const val PUBLIC_POINT_SIZE = 65