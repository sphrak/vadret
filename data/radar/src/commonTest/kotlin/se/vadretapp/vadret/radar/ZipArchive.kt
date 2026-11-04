package se.vadretapp.vadret.radar

import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

private const val LOCAL_FILE_HEADER_SIGNATURE = 0x04034b50
private const val CENTRAL_DIRECTORY_HEADER_SIGNATURE = 0x02014b50
private const val END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054b50
private const val ZIP_VERSION = 20
private const val NO_FLAGS = 0
private const val STORED = 0
private const val MIDNIGHT = 0
private const val NOTHING = 0
private const val FIRST_DISK = 0
private const val CRC_POLYNOMIAL = 0xEDB88320.toInt()
private const val INITIAL_CRC = -1
private const val BITS_PER_BYTE = 8
private const val BYTE_MASK = 0xFF
private const val LOWEST_BIT = 1
private const val DOS_EPOCH_YEAR = 2026 - 1980
private const val DOS_YEAR_SHIFT = 9
private const val DOS_MONTH_SHIFT = 5
private const val NOVEMBER = 11
private const val SECOND = 2

private val dosDate: Int = (DOS_EPOCH_YEAR shl DOS_YEAR_SHIFT) or (NOVEMBER shl DOS_MONTH_SHIFT) or SECOND

internal class ArchivedFile(
    val name: String,
    val bytes: ByteArray,
    val declaredSize: Int = bytes.size
)

// Stored without compression, which okio reads like the deflated archives SMHI serves.
internal fun zipArchive(files: List<ArchivedFile>): ByteArray {
    val buffer = Buffer()
    val localHeaderOffsets: List<Long> = files.map { archivedFile: ArchivedFile ->
        val localHeaderOffset: Long = buffer.size
        buffer.writeIntLe(LOCAL_FILE_HEADER_SIGNATURE)
        buffer.writeShortLe(ZIP_VERSION)
        buffer.writeFileDescription(archivedFile = archivedFile)
        buffer.writeShortLe(NOTHING)
        buffer.write(archivedFile.name.encodeUtf8())
        buffer.write(archivedFile.bytes)
        localHeaderOffset
    }
    val centralDirectoryOffset: Long = buffer.size
    files.zip(localHeaderOffsets).forEach { (archivedFile: ArchivedFile, localHeaderOffset: Long) ->
        buffer.writeIntLe(CENTRAL_DIRECTORY_HEADER_SIGNATURE)
        buffer.writeShortLe(ZIP_VERSION)
        buffer.writeShortLe(ZIP_VERSION)
        buffer.writeFileDescription(archivedFile = archivedFile)
        buffer.writeShortLe(NOTHING)
        buffer.writeShortLe(NOTHING)
        buffer.writeShortLe(FIRST_DISK)
        buffer.writeShortLe(NOTHING)
        buffer.writeIntLe(NOTHING)
        buffer.writeIntLe(localHeaderOffset.toInt())
        buffer.write(archivedFile.name.encodeUtf8())
    }
    val centralDirectorySize: Long = buffer.size - centralDirectoryOffset
    buffer.writeIntLe(END_OF_CENTRAL_DIRECTORY_SIGNATURE)
    buffer.writeShortLe(FIRST_DISK)
    buffer.writeShortLe(FIRST_DISK)
    buffer.writeShortLe(files.size)
    buffer.writeShortLe(files.size)
    buffer.writeIntLe(centralDirectorySize.toInt())
    buffer.writeIntLe(centralDirectoryOffset.toInt())
    buffer.writeShortLe(NOTHING)
    return buffer.readByteArray()
}

private fun Buffer.writeFileDescription(archivedFile: ArchivedFile) {
    val name: ByteString = archivedFile.name.encodeUtf8()
    writeShortLe(NO_FLAGS)
    writeShortLe(STORED)
    writeShortLe(MIDNIGHT)
    writeShortLe(dosDate)
    writeIntLe(archivedFile.bytes.crc32())
    writeIntLe(archivedFile.bytes.size)
    writeIntLe(archivedFile.declaredSize)
    writeShortLe(name.size)
}

private fun ByteArray.crc32(): Int {
    var crc: Int = INITIAL_CRC
    forEach { byte: Byte ->
        crc = crc xor (byte.toInt() and BYTE_MASK)
        repeat(times = BITS_PER_BYTE) {
            crc = if ((crc and LOWEST_BIT) != 0) {
                (crc ushr LOWEST_BIT) xor CRC_POLYNOMIAL
            } else {
                crc ushr LOWEST_BIT
            }
        }
    }
    return crc.inv()
}