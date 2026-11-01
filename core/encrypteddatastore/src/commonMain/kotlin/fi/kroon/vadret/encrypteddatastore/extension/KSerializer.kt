package fi.kroon.vadret.encrypteddatastore.extension

import androidx.datastore.core.CorruptionException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal fun <T> KSerializer<T>.encode(value: T): ByteArray =
    json.encodeToString(
        serializer = this,
        value = value
    ).encodeToByteArray()

internal fun <T> KSerializer<T>.decode(
    bytes: ByteArray,
    fileName: String
): T =
    try {
        json.decodeFromString(
            deserializer = this,
            string = bytes.decodeToString()
        )
    } catch (exception: SerializationException) {
        throw CorruptionException(message = "$fileName holds no readable value: ${exception::class.simpleName}")
    }

private val json: Json = Json { ignoreUnknownKeys = true }