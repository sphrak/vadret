package se.vadretapp.vadret.pushalert.local

import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.KSerializer
import okio.BufferedSink
import okio.BufferedSource
import se.vadretapp.vadret.encryption.DeviceCipher
import se.vadretapp.vadret.pushalert.extension.decode
import se.vadretapp.vadret.pushalert.extension.encode
import se.vadretapp.vadret.pushalert.extension.orThrow

internal class DeviceCipherSerializer<T>(
    private val serializer: KSerializer<T>,
    override val defaultValue: T,
    private val deviceCipher: DeviceCipher,
    private val fileName: String
) : OkioSerializer<T> {
    private val associatedData: ByteArray = fileName.encodeToByteArray()

    override suspend fun readFrom(source: BufferedSource): T {
        val ciphertext = source.readByteArray()
        if (ciphertext.isEmpty()) {
            return defaultValue
        }
        val plaintext = deviceCipher.decrypt(
            ciphertext = ciphertext,
            associatedData = associatedData
        ).orThrow(fileName = fileName)
        return serializer.decode(
            bytes = plaintext,
            fileName = fileName
        )
    }

    override suspend fun writeTo(
        t: T,
        sink: BufferedSink
    ) {
        val ciphertext = deviceCipher.encrypt(
            plaintext = serializer.encode(value = t),
            associatedData = associatedData
        ).orThrow(fileName = fileName)
        sink.write(ciphertext)
    }
}