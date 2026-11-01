package fi.kroon.vadret.encrypteddatastore

import androidx.datastore.core.Serializer
import fi.kroon.vadret.encrypteddatastore.extension.decode
import fi.kroon.vadret.encrypteddatastore.extension.encode
import kotlinx.serialization.KSerializer
import java.io.InputStream
import java.io.OutputStream

internal class JsonSerializer<T>(
    private val serializer: KSerializer<T>,
    override val defaultValue: T,
    private val fileName: String
) : Serializer<T> {
    override suspend fun readFrom(input: InputStream): T {
        val bytes = input.readBytes()
        if (bytes.isEmpty()) {
            return defaultValue
        }
        return serializer.decode(
            bytes = bytes,
            fileName = fileName
        )
    }

    override suspend fun writeTo(
        t: T,
        output: OutputStream
    ) {
        output.write(serializer.encode(value = t))
    }
}