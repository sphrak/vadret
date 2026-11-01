package se.vadretapp.vadret.radar.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

private const val BYTES_PER_ARGB_PIXEL = 4
private const val BLUE_OFFSET = 0
private const val GREEN_OFFSET = 1
private const val RED_OFFSET = 2
private const val ALPHA_OFFSET = 3
private const val GREEN_SHIFT = 8
private const val RED_SHIFT = 16
private const val ALPHA_SHIFT = 24
private const val BYTE_MASK = 0xFF

internal actual fun argbImageBitmap(
    pixels: IntArray,
    width: Int,
    height: Int
): ImageBitmap {
    val bytes = ByteArray(size = pixels.size * BYTES_PER_ARGB_PIXEL)
    pixels.forEachIndexed { index: Int, argb: Int ->
        val offset: Int = index * BYTES_PER_ARGB_PIXEL
        bytes[offset + BLUE_OFFSET] = (argb and BYTE_MASK).toByte()
        bytes[offset + GREEN_OFFSET] = ((argb shr GREEN_SHIFT) and BYTE_MASK).toByte()
        bytes[offset + RED_OFFSET] = ((argb shr RED_SHIFT) and BYTE_MASK).toByte()
        bytes[offset + ALPHA_OFFSET] = ((argb ushr ALPHA_SHIFT) and BYTE_MASK).toByte()
    }
    return immutableImageBitmap(
        pixels = bytes,
        imageInfo = ImageInfo(
            width = width,
            height = height,
            colorType = ColorType.BGRA_8888,
            alphaType = ColorAlphaType.UNPREMUL
        ),
        rowBytes = width * BYTES_PER_ARGB_PIXEL
    )
}

internal actual fun alphaImageBitmap(
    levels: ByteArray,
    width: Int,
    height: Int
): ImageBitmap =
    immutableImageBitmap(
        pixels = levels,
        imageInfo = ImageInfo(
            width = width,
            height = height,
            colorType = ColorType.ALPHA_8,
            alphaType = ColorAlphaType.PREMUL
        ),
        rowBytes = width
    )

private fun immutableImageBitmap(
    pixels: ByteArray,
    imageInfo: ImageInfo,
    rowBytes: Int
): ImageBitmap {
    val bitmap = Bitmap()
    bitmap.allocPixels(imageInfo = imageInfo)
    bitmap.installPixels(
        info = imageInfo,
        pixels = pixels,
        rowBytes = rowBytes
    )
    bitmap.setImmutable()
    return bitmap.asComposeImageBitmap()
}