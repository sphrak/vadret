package se.vadretapp.vadret.radar.extension

import androidx.compose.ui.graphics.ImageBitmap
import se.vadretapp.vadret.radar.image.argbImageBitmap
import se.vadretapp.vadret.radar.projection.ReprojectedImage

private const val BYTE_MASK = 0xFF

internal fun ReprojectedImage.colored(palette: IntArray): ImageBitmap =
    argbImageBitmap(
        pixels = IntArray(size = echoes.size) { index: Int -> palette[echoes[index].toInt() and BYTE_MASK] },
        width = width,
        height = height
    )