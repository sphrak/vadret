package se.vadretapp.vadret.radar.image

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.test.Test
import kotlin.test.assertEquals

private const val ALPHA_SHIFT = 24
private const val BYTE_MASK = 0xFF

class RadarImageBitmapTest {

    private fun ImageBitmap.pixels(): List<Int> {
        val buffer = IntArray(size = width * height)
        readPixels(buffer = buffer)
        return buffer.toList()
    }

    @Test
    fun `opaque argb pixels read back unchanged in their place`() {
        val pixels: IntArray = intArrayOf(
            0xFF102030.toInt(),
            0xFF405060.toInt(),
            0xFF708090.toInt(),
            0xFFA0B0C0.toInt(),
            0xFFD0E0F0.toInt(),
            0xFF000000.toInt()
        )

        val imageBitmap: ImageBitmap = argbImageBitmap(
            pixels = pixels,
            width = 3,
            height = 2
        )

        assertEquals(expected = listOf(3, 2), actual = listOf(imageBitmap.width, imageBitmap.height))
        assertEquals(expected = pixels.toList(), actual = imageBitmap.pixels())
    }

    @Test
    fun `echo levels become the alpha of each pixel`() {
        val levels: ByteArray = byteArrayOf(0, 1, 127, -128, -1, 42)

        val imageBitmap: ImageBitmap = alphaImageBitmap(
            levels = levels,
            width = 2,
            height = 3
        )

        assertEquals(
            expected = levels.map { level: Byte -> level.toInt() and BYTE_MASK },
            actual = imageBitmap.pixels().map { pixel: Int -> (pixel ushr ALPHA_SHIFT) and BYTE_MASK }
        )
    }
}