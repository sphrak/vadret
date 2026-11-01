package fi.kroon.vadret.encrypteddatastore

import com.google.crypto.tink.Aead
import fi.kroon.vadret.encrypteddatastore.extension.orThrow
import fi.kroon.vadret.encryption.DeviceCipher

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