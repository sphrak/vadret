package se.vadretapp.vadret.radar.tiff

import se.vadretapp.vadret.radar.exception.TiffFormatException

private const val BYTE_MASK = 0xFF
private const val BITS_PER_BYTE = 8

internal class TiffBytes(
    private val bytes: ByteArray,
    private val isLittleEndian: Boolean
) {

    val size: Int = bytes.size

    fun unsignedShort(offset: Int): Int =
        unsigned(
            offset = offset,
            byteCount = Short.SIZE_BYTES
        )

    fun int(offset: Int): Int =
        unsigned(
            offset = offset,
            byteCount = Int.SIZE_BYTES
        )

    fun slice(
        offset: Int,
        byteCount: Int
    ): ByteArray {
        checkRange(
            offset = offset,
            byteCount = byteCount
        )
        return bytes.copyOfRange(fromIndex = offset, toIndex = offset + byteCount)
    }

    private fun unsigned(
        offset: Int,
        byteCount: Int
    ): Int {
        checkRange(
            offset = offset,
            byteCount = byteCount
        )
        var value = 0
        for (position in 0 until byteCount) {
            val byteIndex: Int = if (isLittleEndian) {
                offset + byteCount - 1 - position
            } else {
                offset + position
            }
            value = (value shl BITS_PER_BYTE) or (bytes[byteIndex].toInt() and BYTE_MASK)
        }
        return value
    }

    private fun checkRange(
        offset: Int,
        byteCount: Int
    ) {
        if (offset < 0 || byteCount < 0 || offset > bytes.size - byteCount) {
            throw TiffFormatException(message = "Reading $byteCount bytes at $offset runs past the end of ${bytes.size} bytes")
        }
    }
}