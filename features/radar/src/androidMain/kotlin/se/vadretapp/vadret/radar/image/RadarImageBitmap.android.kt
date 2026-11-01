package se.vadretapp.vadret.radar.image

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import java.nio.ByteBuffer

internal actual fun argbImageBitmap(
    pixels: IntArray,
    width: Int,
    height: Int
): ImageBitmap =
    Bitmap
        .createBitmap(
            pixels,
            width,
            height,
            Bitmap.Config.ARGB_8888
        ).asImageBitmap()

internal actual fun alphaImageBitmap(
    levels: ByteArray,
    width: Int,
    height: Int
): ImageBitmap {
    val bitmap: Bitmap = createBitmap(
        width = width,
        height = height,
        config = Bitmap.Config.ALPHA_8
    )
    val buffer: ByteBuffer = ByteBuffer.allocate(bitmap.rowBytes * height)
    for (row in 0 until height) {
        buffer.position(row * bitmap.rowBytes)
        buffer.put(
            levels,
            row * width,
            width
        )
    }
    buffer.rewind()
    bitmap.copyPixelsFromBuffer(buffer)
    return bitmap.asImageBitmap()
}