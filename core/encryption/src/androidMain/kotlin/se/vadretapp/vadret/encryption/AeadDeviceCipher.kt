package se.vadretapp.vadret.encryption

import com.google.crypto.tink.Aead
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.encryption.error.EncryptionError
import se.vadretapp.vadret.encryption.model.KeyAlias
import se.vadretapp.vadret.logging.Logger
import se.vadretapp.vadret.logging.LoggerFactory
import java.security.GeneralSecurityException
import java.security.ProviderException
import javax.crypto.BadPaddingException

internal class AeadDeviceCipher(
    private val keyAlias: KeyAlias,
    produceAead: () -> Aead
) : DeviceCipher {
    private val aead: Aead by lazy(initializer = produceAead)

    override fun encrypt(
        plaintext: ByteArray,
        associatedData: ByteArray
    ): Either<EncryptionError, ByteArray> =
        try {
            aead.encrypt(plaintext, associatedData).asRight()
        } catch (exception: GeneralSecurityException) {
            unavailable(operation = ENCRYPT, description = exception.message)
        } catch (exception: ProviderException) {
            unavailable(operation = ENCRYPT, description = exception.message)
        }

    override fun decrypt(
        ciphertext: ByteArray,
        associatedData: ByteArray
    ): Either<EncryptionError, ByteArray> =
        try {
            aead.decrypt(ciphertext, associatedData).asRight()
        } catch (exception: BadPaddingException) {
            logger.warn { "${EncryptionError.NOT_FOR_THIS_KEY}: ${keyAlias.value} did not make this ciphertext: ${exception.message}" }
            EncryptionError.NOT_FOR_THIS_KEY.asLeft()
        } catch (exception: GeneralSecurityException) {
            unavailable(operation = DECRYPT, description = exception.message)
        } catch (exception: ProviderException) {
            unavailable(operation = DECRYPT, description = exception.message)
        }

    private fun unavailable(
        operation: String,
        description: String?
    ): Either<EncryptionError, Nothing> {
        logger.warn { "${EncryptionError.KEY_STORE_UNAVAILABLE}: the Keystore could not $operation with ${keyAlias.value}: $description" }
        return EncryptionError.KEY_STORE_UNAVAILABLE.asLeft()
    }
}

private val logger: Logger = LoggerFactory.getLogger(tag = "se.vadretapp.vadret.encryption")

private const val ENCRYPT = "encrypt"
private const val DECRYPT = "decrypt"