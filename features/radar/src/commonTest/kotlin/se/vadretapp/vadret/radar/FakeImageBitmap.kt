package se.vadretapp.vadret.radar

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import se.vadretapp.vadret.radar.precipitation.RadarEchoes
import se.vadretapp.vadret.radar.projection.ReprojectedImage

private const val FAKE_SIZE = 1

internal class FakeImageBitmap : ImageBitmap {

    override val width: Int = FAKE_SIZE
    override val height: Int = FAKE_SIZE
    override val colorSpace: ColorSpace = ColorSpaces.Srgb
    override val hasAlpha: Boolean = true
    override val config: ImageBitmapConfig = ImageBitmapConfig.Argb8888

    override fun readPixels(
        buffer: IntArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        bufferOffset: Int,
        stride: Int
    ) = Unit

    override fun prepareToDraw() = Unit
}

internal fun fakeRadarEchoes(): RadarEchoes =
    RadarEchoes(
        reprojectedImage = ReprojectedImage(
            echoes = ByteArray(size = FAKE_SIZE),
            width = FAKE_SIZE,
            height = FAKE_SIZE
        ),
        image = FakeImageBitmap()
    )