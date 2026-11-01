package se.vadretapp.vadret.radar

import coil3.Canvas
import coil3.Image
import se.vadretapp.vadret.radar.precipitation.RadarEchoes

private const val ECHO_BYTES_PER_PIXEL = 1
private const val ALPHA_BYTES_PER_PIXEL = 1
private const val COLORED_BYTES_PER_PIXEL = 4
private const val BYTES_PER_PIXEL = ECHO_BYTES_PER_PIXEL + ALPHA_BYTES_PER_PIXEL + COLORED_BYTES_PER_PIXEL

internal class RadarEchoesImage(
    val radarEchoes: RadarEchoes
) : Image {

    override val size: Long = radarEchoes.reprojectedImage.echoes.size.toLong() * BYTES_PER_PIXEL
    override val width: Int = radarEchoes.reprojectedImage.width
    override val height: Int = radarEchoes.reprojectedImage.height
    override val shareable: Boolean = true

    override fun draw(canvas: Canvas) = Unit
}