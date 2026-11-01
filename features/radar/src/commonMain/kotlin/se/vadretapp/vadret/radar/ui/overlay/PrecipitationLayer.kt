package se.vadretapp.vadret.radar.ui.overlay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import se.vadretapp.vadret.radar.extension.toImageBitmap
import se.vadretapp.vadret.radar.precipitation.MotionField
import se.vadretapp.vadret.radar.precipitation.PrecipitationMotion
import se.vadretapp.vadret.radar.precipitation.PrecipitationOverlay
import se.vadretapp.vadret.radar.ui.map.RasterProjection

private const val STILL_CELLS = 1
private const val NO_DISPLACEMENT = 0f
private const val NO_PROGRESS = 0f
private const val RASTER_ORIGIN = 0f

internal class PrecipitationLayer(
    private val precipitationShader: PrecipitationShader
) {

    private val stillMotion: ImageBitmap = MotionField(
        columns = STILL_CELLS,
        rows = STILL_CELLS,
        eastward = floatArrayOf(NO_DISPLACEMENT),
        southward = floatArrayOf(NO_DISPLACEMENT)
    ).toImageBitmap()

    fun DrawScope.drawPrecipitation(
        screenProjection: ScreenProjection,
        echoesOverlay: PrecipitationOverlay.Echoes,
        progress: Float
    ) {
        val echoes: ImageBitmap = echoesOverlay.echoes
        val motion: PrecipitationMotion? = echoesOverlay.motion
        val rasterProjection: RasterProjection = screenProjection.toRasterProjection(
            rasterWidth = echoes.width,
            rasterHeight = echoes.height
        ) ?: return
        val brush = ShaderBrush(
            shader = precipitationShader.shader(
                echoes = echoes,
                nextEchoes = motion?.nextEchoes ?: echoes,
                motionField = motion?.field ?: stillMotion,
                progress = if (motion == null) {
                    NO_PROGRESS
                } else {
                    progress
                }
            )
        )
        val left: Float = rasterProjection.screenX(rasterX = RASTER_ORIGIN)
        val top: Float = rasterProjection.screenY(rasterY = RASTER_ORIGIN)
        val width: Float = echoes.width.toFloat()
        val height: Float = echoes.height.toFloat()
        withTransform(
            transformBlock = {
                translate(
                    left = left,
                    top = top
                )
                scale(
                    scaleX = (rasterProjection.screenX(rasterX = width) - left) / width,
                    scaleY = (rasterProjection.screenY(rasterY = height) - top) / height,
                    pivot = Offset.Zero
                )
            }
        ) {
            drawRect(
                brush = brush,
                size = Size(
                    width = width,
                    height = height
                )
            )
        }
    }
}