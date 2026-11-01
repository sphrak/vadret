package se.vadretapp.vadret.radar.tiff

import se.vadretapp.vadret.radar.exception.TiffFormatException
import se.vadretapp.vadret.radar.projection.RadarRaster

private const val LITTLE_ENDIAN_MARK = 0x4949
private const val BIG_ENDIAN_MARK = 0x4D4D
private const val TIFF_MAGIC = 42
private const val MAGIC_OFFSET = 2
private const val DIRECTORY_OFFSET_POSITION = 4
private const val DIRECTORY_COUNT_SIZE = 2
private const val DIRECTORY_ENTRY_SIZE = 12
private const val ENTRY_TYPE_OFFSET = 2
private const val ENTRY_COUNT_OFFSET = 4
private const val ENTRY_VALUE_OFFSET = 8
private const val INLINE_VALUE_SIZE = 4

private const val TAG_IMAGE_WIDTH = 256
private const val TAG_IMAGE_LENGTH = 257
private const val TAG_BITS_PER_SAMPLE = 258
private const val TAG_COMPRESSION = 259
private const val TAG_STRIP_OFFSETS = 273
private const val TAG_SAMPLES_PER_PIXEL = 277
private const val TAG_ROWS_PER_STRIP = 278
private const val TAG_STRIP_BYTE_COUNTS = 279
private const val TAG_PREDICTOR = 317

private const val TYPE_SHORT = 3
private const val TYPE_LONG = 4

private const val DEFAULT_BITS_PER_SAMPLE = 1
private const val SUPPORTED_BITS_PER_SAMPLE = 8
private const val SUPPORTED_SAMPLES_PER_PIXEL = 1
private const val PREDICTOR_NONE = 1
private const val COMPRESSION_NONE = 1
private const val COMPRESSION_LZW = 5
private const val MAXIMUM_DIMENSION = 4096

internal object TiffReader {

    fun read(bytes: ByteArray): RadarRaster {
        val tiffBytes = TiffBytes(
            bytes = bytes,
            isLittleEndian = isLittleEndian(bytes = bytes)
        )
        if (tiffBytes.unsignedShort(offset = MAGIC_OFFSET) != TIFF_MAGIC) {
            throw TiffFormatException(message = "The TIFF magic number is missing")
        }
        val tags: Map<Int, IntArray> = readTags(
            tiffBytes = tiffBytes,
            directoryOffset = tiffBytes.int(offset = DIRECTORY_OFFSET_POSITION)
        )
        checkSupported(tags = tags)
        val width: Int = tags.dimension(tag = TAG_IMAGE_WIDTH)
        val height: Int = tags.dimension(tag = TAG_IMAGE_LENGTH)
        return RadarRaster(
            values = readStrips(
                tiffBytes = tiffBytes,
                tags = tags,
                width = width,
                height = height
            ),
            width = width,
            height = height
        )
    }

    private fun isLittleEndian(bytes: ByteArray): Boolean =
        when (TiffBytes(bytes = bytes, isLittleEndian = false).unsignedShort(offset = 0)) {
            LITTLE_ENDIAN_MARK -> true
            BIG_ENDIAN_MARK -> false
            else -> throw TiffFormatException(message = "The byte order mark is missing")
        }

    private fun readTags(
        tiffBytes: TiffBytes,
        directoryOffset: Int
    ): Map<Int, IntArray> {
        val entryCount: Int = tiffBytes.unsignedShort(offset = directoryOffset)
        return (0 until entryCount).associate { entryIndex: Int ->
            val entryOffset: Int = directoryOffset + DIRECTORY_COUNT_SIZE + entryIndex * DIRECTORY_ENTRY_SIZE
            tiffBytes.unsignedShort(offset = entryOffset) to readValues(
                tiffBytes = tiffBytes,
                entryOffset = entryOffset
            )
        }
    }

    private fun readValues(
        tiffBytes: TiffBytes,
        entryOffset: Int
    ): IntArray {
        val valueSize: Int = when (tiffBytes.unsignedShort(offset = entryOffset + ENTRY_TYPE_OFFSET)) {
            TYPE_SHORT -> Short.SIZE_BYTES
            TYPE_LONG -> Int.SIZE_BYTES
            else -> return IntArray(size = 0)
        }
        val count: Int = tiffBytes.int(offset = entryOffset + ENTRY_COUNT_OFFSET)
        if (count < 0 || count > tiffBytes.size / valueSize) {
            throw TiffFormatException(message = "A tag claims $count values")
        }
        val valuesOffset: Int = if (count * valueSize <= INLINE_VALUE_SIZE) {
            entryOffset + ENTRY_VALUE_OFFSET
        } else {
            tiffBytes.int(offset = entryOffset + ENTRY_VALUE_OFFSET)
        }
        return IntArray(size = count) { valueIndex: Int ->
            val offset: Int = valuesOffset + valueIndex * valueSize
            if (valueSize == Short.SIZE_BYTES) {
                tiffBytes.unsignedShort(offset = offset)
            } else {
                tiffBytes.int(offset = offset)
            }
        }
    }

    private fun checkSupported(tags: Map<Int, IntArray>) {
        val bitsPerSample: Int = tags.value(tag = TAG_BITS_PER_SAMPLE, default = DEFAULT_BITS_PER_SAMPLE)
        val samplesPerPixel: Int = tags.value(tag = TAG_SAMPLES_PER_PIXEL, default = SUPPORTED_SAMPLES_PER_PIXEL)
        val predictor: Int = tags.value(tag = TAG_PREDICTOR, default = PREDICTOR_NONE)
        val compression: Int = tags.value(tag = TAG_COMPRESSION, default = COMPRESSION_NONE)
        if (bitsPerSample != SUPPORTED_BITS_PER_SAMPLE || samplesPerPixel != SUPPORTED_SAMPLES_PER_PIXEL) {
            throw TiffFormatException(message = "Only 8 bit single channel images are read, not $samplesPerPixel x $bitsPerSample bits")
        }
        if (predictor != PREDICTOR_NONE) {
            throw TiffFormatException(message = "Predictor $predictor is not read")
        }
        if (compression != COMPRESSION_NONE && compression != COMPRESSION_LZW) {
            throw TiffFormatException(message = "Compression $compression is not read")
        }
    }

    private fun readStrips(
        tiffBytes: TiffBytes,
        tags: Map<Int, IntArray>,
        width: Int,
        height: Int
    ): ByteArray {
        val rowsPerStrip: Int = tags.value(tag = TAG_ROWS_PER_STRIP, default = height).coerceIn(minimumValue = 1, maximumValue = height)
        val stripCount: Int = (height + rowsPerStrip - 1) / rowsPerStrip
        val stripOffsets: IntArray = tags.required(tag = TAG_STRIP_OFFSETS)
        val stripByteCounts: IntArray = tags.required(tag = TAG_STRIP_BYTE_COUNTS)
        if (stripOffsets.size != stripCount || stripByteCounts.size != stripCount) {
            throw TiffFormatException(message = "Expected $stripCount strips, found ${stripOffsets.size} offsets and ${stripByteCounts.size} sizes")
        }
        val compression: Int = tags.value(tag = TAG_COMPRESSION, default = COMPRESSION_NONE)
        val values = ByteArray(size = width * height)
        for (strip in 0 until stripCount) {
            val firstRow: Int = strip * rowsPerStrip
            val pixelCount: Int = minOf(rowsPerStrip, height - firstRow) * width
            decodeStrip(
                compression = compression,
                stripBytes = tiffBytes.slice(offset = stripOffsets[strip], byteCount = stripByteCounts[strip]),
                pixelCount = pixelCount
            ).copyInto(destination = values, destinationOffset = firstRow * width, endIndex = pixelCount)
        }
        return values
    }

    private fun decodeStrip(
        compression: Int,
        stripBytes: ByteArray,
        pixelCount: Int
    ): ByteArray =
        when {
            compression == COMPRESSION_LZW -> LzwDecoder(input = stripBytes, outputSize = pixelCount).decode()
            stripBytes.size >= pixelCount -> stripBytes
            else -> throw TiffFormatException(message = "A strip holds ${stripBytes.size} of $pixelCount bytes")
        }

    private fun Map<Int, IntArray>.required(tag: Int): IntArray = this[tag] ?: throw TiffFormatException(message = "Tag $tag is missing")

    private fun Map<Int, IntArray>.value(
        tag: Int,
        default: Int
    ): Int {
        val values: IntArray = this[tag] ?: return default
        if (values.size != 1) {
            throw TiffFormatException(message = "Tag $tag holds ${values.size} values")
        }
        return values.first()
    }

    private fun Map<Int, IntArray>.dimension(tag: Int): Int {
        val dimension: Int = value(tag = tag, default = 0)
        if (dimension !in 1..MAXIMUM_DIMENSION) {
            throw TiffFormatException(message = "Tag $tag gives a dimension of $dimension")
        }
        return dimension
    }
}