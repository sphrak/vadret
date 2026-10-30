package fi.kroon.vadret.radar.tiff

import fi.kroon.vadret.radar.exception.TiffFormatException

private const val CLEAR_CODE = 256
private const val END_OF_INFORMATION = 257
private const val FIRST_FREE_CODE = 258
private const val LITERAL_COUNT = 256
private const val MINIMUM_CODE_WIDTH = 9
private const val MAXIMUM_CODE_WIDTH = 12
private const val TABLE_SIZE = 1 shl MAXIMUM_CODE_WIDTH
private const val NO_CODE = -1
private const val BYTE_MASK = 0xFF
private const val BITS_PER_BYTE = 8
private const val HIGHEST_BIT = 7

internal class LzwDecoder(
    private val input: ByteArray,
    outputSize: Int
) {

    private val output = ByteArray(size = outputSize)
    private val prefixes = IntArray(size = TABLE_SIZE) { NO_CODE }
    private val suffixes = ByteArray(size = TABLE_SIZE) { code: Int -> code.toByte() }
    private val firstBytes = ByteArray(size = TABLE_SIZE) { code: Int -> code.toByte() }
    private val lengths = IntArray(size = TABLE_SIZE) { 1 }
    private var outputPosition = 0
    private var bitPosition = 0L
    private var nextCode = FIRST_FREE_CODE
    private var codeWidth = MINIMUM_CODE_WIDTH

    fun decode(): ByteArray {
        var previousCode: Int = NO_CODE
        while (outputPosition < output.size) {
            val code: Int = readCode()
            when {
                code == END_OF_INFORMATION -> throw TiffFormatException(message = "LZW data ended after $outputPosition of ${output.size} bytes")
                code == CLEAR_CODE -> {
                    nextCode = FIRST_FREE_CODE
                    codeWidth = MINIMUM_CODE_WIDTH
                    previousCode = NO_CODE
                }
                previousCode == NO_CODE -> {
                    if (code >= LITERAL_COUNT) {
                        throw TiffFormatException(message = "LZW code $code follows a clear code")
                    }
                    write(code = code)
                    previousCode = code
                }
                code < nextCode -> {
                    write(code = code)
                    addEntry(prefixCode = previousCode, suffix = firstBytes[code])
                    previousCode = code
                }
                code == nextCode -> {
                    addEntry(prefixCode = previousCode, suffix = firstBytes[previousCode])
                    write(code = code)
                    previousCode = code
                }
                else -> throw TiffFormatException(message = "LZW code $code is not in the table")
            }
        }
        return output
    }

    private fun readCode(): Int {
        if (bitPosition + codeWidth > input.size.toLong() * BITS_PER_BYTE) {
            throw TiffFormatException(message = "LZW data ran out after $outputPosition of ${output.size} bytes")
        }
        var code = 0
        repeat(times = codeWidth) {
            val byte: Int = input[(bitPosition / BITS_PER_BYTE).toInt()].toInt() and BYTE_MASK
            val bit: Int = (byte shr (HIGHEST_BIT - (bitPosition % BITS_PER_BYTE).toInt())) and 1
            code = (code shl 1) or bit
            bitPosition++
        }
        return code
    }

    private fun write(code: Int) {
        val length: Int = lengths[code]
        var position: Int = outputPosition + length - 1
        var current: Int = code
        while (current != NO_CODE) {
            if (position < output.size) {
                output[position] = suffixes[current]
            }
            current = prefixes[current]
            position--
        }
        outputPosition = minOf(outputPosition + length, output.size)
    }

    private fun addEntry(
        prefixCode: Int,
        suffix: Byte
    ) {
        if (nextCode >= TABLE_SIZE) {
            return
        }
        prefixes[nextCode] = prefixCode
        suffixes[nextCode] = suffix
        firstBytes[nextCode] = firstBytes[prefixCode]
        lengths[nextCode] = lengths[prefixCode] + 1
        nextCode++
        if (nextCode + 1 >= 1 shl codeWidth && codeWidth < MAXIMUM_CODE_WIDTH) {
            codeWidth++
        }
    }
}