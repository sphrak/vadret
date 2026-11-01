package se.vadretapp.vadret.radar.precipitation

import androidx.compose.ui.graphics.ImageBitmap
import se.vadretapp.vadret.radar.extension.colored
import se.vadretapp.vadret.radar.projection.PrecipitationColorScale
import se.vadretapp.vadret.radar.projection.ReprojectedImage

private val precipitationPalette: IntArray by lazy { PrecipitationColorScale.palette() }

internal class RadarEchoes(
    val reprojectedImage: ReprojectedImage,
    val image: ImageBitmap
) {

    val coloredImage: ImageBitmap by lazy { reprojectedImage.colored(palette = precipitationPalette) }
}