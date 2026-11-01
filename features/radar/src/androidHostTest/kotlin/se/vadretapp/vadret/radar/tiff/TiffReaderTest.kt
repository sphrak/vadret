package se.vadretapp.vadret.radar.tiff

import se.vadretapp.vadret.radar.exception.TiffFormatException
import se.vadretapp.vadret.radar.projection.RadarRaster
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private const val TYPE_SHORT = 3
private const val TYPE_LONG = 4
private const val TYPE_DOUBLE = 12
private const val HEADER_SIZE = 8
private const val ENTRY_SIZE = 12
private const val BYTE_MASK = 0xFF

private class TiffEntry(
    val tag: Int,
    val type: Int,
    val values: IntArray
)

private fun tiff(
    byteOrder: ByteOrder,
    entries: List<TiffEntry>,
    imageData: ByteArray
): ByteArray {
    val directoryOffset: Int = HEADER_SIZE + imageData.size
    val extraOffset: Int = directoryOffset + Short.SIZE_BYTES + entries.size * ENTRY_SIZE + Int.SIZE_BYTES
    val extraSize: Int = entries.sumOf { entry: TiffEntry -> entry.outOfLineSize() }
    val buffer: ByteBuffer = ByteBuffer.allocate(extraOffset + extraSize).order(byteOrder)
    val byteOrderMark: ByteArray = if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
        "II".toByteArray()
    } else {
        "MM".toByteArray()
    }
    buffer.put(byteOrderMark)
    buffer.putShort(42)
    buffer.putInt(directoryOffset)
    buffer.put(imageData)
    buffer.putShort(entries.size.toShort())
    var nextExtra: Int = extraOffset
    entries.forEach { entry: TiffEntry ->
        buffer.putShort(entry.tag.toShort())
        buffer.putShort(entry.type.toShort())
        buffer.putInt(entry.values.size)
        if (entry.byteSize() <= Int.SIZE_BYTES) {
            val valueStart: Int = buffer.position()
            entry.values.forEach { value: Int -> buffer.putValue(type = entry.type, value = value) }
            buffer.position(valueStart + Int.SIZE_BYTES)
        } else {
            buffer.putInt(nextExtra)
            val entryEnd: Int = buffer.position()
            buffer.position(nextExtra)
            entry.values.forEach { value: Int -> buffer.putValue(type = entry.type, value = value) }
            nextExtra = buffer.position()
            buffer.position(entryEnd)
        }
    }
    buffer.putInt(0)
    return buffer.array()
}

private fun TiffEntry.byteSize(): Int =
    values.size * when (type) {
        TYPE_SHORT -> Short.SIZE_BYTES
        TYPE_LONG -> Int.SIZE_BYTES
        else -> Long.SIZE_BYTES
    }

private fun TiffEntry.outOfLineSize(): Int =
    if (byteSize() <= Int.SIZE_BYTES) {
        0
    } else {
        byteSize()
    }

private fun ByteBuffer.putValue(
    type: Int,
    value: Int
) {
    when (type) {
        TYPE_SHORT -> putShort(value.toShort())
        TYPE_LONG -> putInt(value)
        else -> putDouble(value.toDouble())
    }
}

private fun short(
    tag: Int,
    vararg values: Int
): TiffEntry = TiffEntry(tag = tag, type = TYPE_SHORT, values = values)

private fun long(
    tag: Int,
    vararg values: Int
): TiffEntry = TiffEntry(tag = tag, type = TYPE_LONG, values = values)

private val fourByFive = ByteArray(size = 20) { index: Int -> (index * 12).toByte() }

private fun uncompressed(
    byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN,
    overrides: List<TiffEntry> = emptyList(),
    removedTags: Set<Int> = emptySet()
): ByteArray {
    val entries: List<TiffEntry> = listOf(
        short(tag = 256, 4),
        long(tag = 257, 5),
        short(tag = 258, 8),
        short(tag = 259, 1),
        long(tag = 273, HEADER_SIZE, HEADER_SIZE + 8, HEADER_SIZE + 16),
        short(tag = 277, 1),
        short(tag = 278, 2),
        long(tag = 279, 8, 8, 4),
        TiffEntry(tag = 33550, type = TYPE_DOUBLE, values = intArrayOf(2014, 2014, 0))
    )
    val tags: Set<Int> = overrides.map { entry: TiffEntry -> entry.tag }.toSet()
    return tiff(
        byteOrder = byteOrder,
        entries = (entries.filterNot { entry: TiffEntry -> entry.tag in tags || entry.tag in removedTags } + overrides).sortedBy { entry: TiffEntry -> entry.tag },
        imageData = fourByFive
    )
}

private fun resource(name: String): ByteArray = requireNotNull(TiffReaderTest::class.java.getResourceAsStream("/$name")).readBytes()

class TiffReaderTest {

    @Test
    fun `strips of an uncompressed image are read in either byte order`() {
        val rasters: List<RadarRaster> = listOf(ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN).map { byteOrder: ByteOrder ->
            TiffReader.read(bytes = uncompressed(byteOrder = byteOrder))
        }

        rasters.forEach { radarRaster: RadarRaster ->
            assertEquals(expected = 4 to 5, actual = radarRaster.width to radarRaster.height)
            assertContentEquals(expected = fourByFive, actual = radarRaster.values)
        }
    }

    @Test
    fun `missing optional tags take their defaults`() {
        val radarRaster: RadarRaster = TiffReader.read(
            bytes = uncompressed(
                overrides = listOf(long(tag = 273, HEADER_SIZE), long(tag = 279, 20)),
                removedTags = setOf(259, 277, 278)
            )
        )

        assertContentEquals(expected = fourByFive, actual = radarRaster.values)
    }

    @Test
    fun `an LZW image spanning several strips and table resets is read exactly`() {
        val radarRaster: RadarRaster = TiffReader.read(bytes = resource(name = "lzw_pattern.tif"))

        assertEquals(expected = 300 to 200, actual = radarRaster.width to radarRaster.height)
        assertContentEquals(
            expected = ByteArray(size = 300 * 200) { index: Int ->
                val x: Int = index % 300
                val y: Int = index / 300
                ((x * x * 31 + y * 17 + x * y) % 251).toByte()
            },
            actual = radarRaster.values
        )
    }

    @Test
    fun `an SMHI radar composite is read like GDAL reads it`() {
        val radarRaster: RadarRaster = TiffReader.read(bytes = resource(name = "smhi_radar_202610250725.tif"))
        val values: List<Long> = radarRaster.values.map { value: Byte -> (value.toInt() and BYTE_MASK).toLong() }

        assertEquals(expected = 471 to 887, actual = radarRaster.width to radarRaster.height)
        assertEquals(expected = 46_741_613L, actual = values.sum())
        assertEquals(expected = 23_368_923_702L, actual = values.withIndex().sumOf { (index: Int, value: Long) -> value * (index % 1000 + 1) })
        assertEquals(expected = 41_265, actual = values.indexOfFirst { value: Long -> value in 1L..254L })
    }

    @Test
    fun `anything that is not a supported TIFF is refused`() {
        val pngHeader: ByteArray = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val wrongMagic: ByteArray = uncompressed()
        wrongMagic[2] = 43
        val absurdCount: ByteArray = uncompressed()
        ByteBuffer.wrap(absurdCount).order(ByteOrder.LITTLE_ENDIAN).putInt(HEADER_SIZE + fourByFive.size + Short.SIZE_BYTES + Int.SIZE_BYTES, Int.MAX_VALUE)
        val refused: List<ByteArray> = listOf(
            pngHeader,
            byteArrayOf(0x49),
            wrongMagic,
            absurdCount,
            uncompressed(overrides = listOf(short(tag = 258, 16))),
            uncompressed(overrides = listOf(short(tag = 277, 3))),
            uncompressed(overrides = listOf(short(tag = 317, 2))),
            uncompressed(overrides = listOf(short(tag = 259, 7))),
            uncompressed(overrides = listOf(short(tag = 258, 8, 8))),
            uncompressed(overrides = listOf(short(tag = 256, 0))),
            uncompressed(overrides = listOf(short(tag = 256, 5000))),
            uncompressed(removedTags = setOf(256)),
            uncompressed(removedTags = setOf(273)),
            uncompressed(overrides = listOf(long(tag = 273, HEADER_SIZE, HEADER_SIZE + 8))),
            uncompressed(overrides = listOf(long(tag = 279, 8, 8, 2))),
            uncompressed(overrides = listOf(long(tag = 279, 8, 8, 4000))),
            uncompressed(overrides = listOf(TiffEntry(tag = 273, type = TYPE_LONG, values = IntArray(size = 3) { -1 })))
        )

        refused.forEach { bytes: ByteArray ->
            assertFailsWith<TiffFormatException> { TiffReader.read(bytes = bytes) }
        }
    }

    @Test
    fun `LZW data that ends or breaks before the strip is full is refused`() {
        val lzw: ByteArray = resource(name = "lzw_pattern.tif")
        val truncated: ByteArray = lzw.copyOf(newSize = lzw.size - 2000)
        val refused: List<ByteArray> = listOf(
            lzwStrip(codes = intArrayOf(256, 65, 257)),
            lzwStrip(codes = intArrayOf(256, 300)),
            lzwStrip(codes = intArrayOf(256, 65, 400)),
            lzwStrip(codes = intArrayOf(256, 65)),
            truncated
        )

        refused.forEach { bytes: ByteArray ->
            assertFailsWith<TiffFormatException> { TiffReader.read(bytes = bytes) }
        }
    }

    @Test
    fun `an LZW strip repeating its newest code decodes it`() {
        val radarRaster: RadarRaster = TiffReader.read(bytes = lzwStrip(codes = intArrayOf(256, 65, 258, 259, 257), pixelCount = 6))

        assertContentEquals(expected = ByteArray(size = 6) { 65 }, actual = radarRaster.values)
    }

    private fun lzwStrip(
        codes: IntArray,
        pixelCount: Int = 4
    ): ByteArray {
        val bits: String = codes.joinToString(separator = "") { code: Int -> code.toString(radix = 2).padStart(length = 9, padChar = '0') }
        val stripBytes = ByteArray(size = (bits.length + 7) / 8) { index: Int ->
            bits.drop(n = index * 8).take(n = 8).padEnd(length = 8, padChar = '0').toInt(radix = 2).toByte()
        }
        return tiff(
            byteOrder = ByteOrder.LITTLE_ENDIAN,
            entries = listOf(
                short(tag = 256, pixelCount),
                short(tag = 257, 1),
                short(tag = 258, 8),
                short(tag = 259, 5),
                long(tag = 273, HEADER_SIZE),
                long(tag = 279, stripBytes.size)
            ),
            imageData = stripBytes
        )
    }
}