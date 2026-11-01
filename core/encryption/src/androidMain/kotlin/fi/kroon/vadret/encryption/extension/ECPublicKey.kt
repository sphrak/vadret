package fi.kroon.vadret.encryption.extension

import fi.kroon.vadret.encryption.COORDINATE_BYTES
import fi.kroon.vadret.encryption.UNCOMPRESSED_POINT_PREFIX
import java.math.BigInteger
import java.security.interfaces.ECPublicKey

internal fun ECPublicKey.toUncompressedPoint(): ByteArray =
    byteArrayOf(UNCOMPRESSED_POINT_PREFIX) + w.affineX.toCoordinateBytes() + w.affineY.toCoordinateBytes()

private fun BigInteger.toCoordinateBytes(): ByteArray {
    val bytes = toByteArray()
    return when {
        bytes.size == COORDINATE_BYTES -> bytes
        bytes.size > COORDINATE_BYTES -> bytes.copyOfRange(fromIndex = bytes.size - COORDINATE_BYTES, toIndex = bytes.size)
        else -> ByteArray(size = COORDINATE_BYTES - bytes.size) + bytes
    }
}