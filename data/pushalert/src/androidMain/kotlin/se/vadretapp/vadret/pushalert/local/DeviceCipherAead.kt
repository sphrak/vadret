package se.vadretapp.vadret.pushalert.local

import com.google.crypto.tink.Aead
import se.vadretapp.vadret.encryption.DeviceCipher
import se.vadretapp.vadret.pushalert.extension.orThrow

internal class DeviceCipherAead(
    private val deviceCipher: DeviceCipher,
    private val fileName: String
) : Aead {
    override fun encrypt(
        plaintext: ByteArray,
        associatedData: ByteArray
    ): ByteArray =
        deviceCipher.encrypt(
            plaintext = plaintext,
            associatedData = associatedData
        ).orThrow(fileName = fileName)

    override fun decrypt(
        ciphertext: ByteArray,
        associatedData: ByteArray
    ): ByteArray =
        deviceCipher.decrypt(
            ciphertext = ciphertext,
            associatedData = associatedData
        ).orThrow(fileName = fileName)
}