package se.vadretapp.vadret.radar.image

import androidx.compose.ui.graphics.ImageBitmap

internal expect fun argbImageBitmap(
    pixels: IntArray,
    width: Int,
    height: Int
): ImageBitmap

internal expect fun alphaImageBitmap(
    levels: ByteArray,
    width: Int,
    height: Int
): ImageBitmap