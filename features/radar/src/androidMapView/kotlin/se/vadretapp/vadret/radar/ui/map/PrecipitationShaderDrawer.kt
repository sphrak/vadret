package se.vadretapp.vadret.radar.ui.map

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.graphics.withTranslation
import org.maplibre.android.maps.Projection
import se.vadretapp.vadret.radar.extension.DISPLACEMENT_PER_CHANNEL_STEP
import se.vadretapp.vadret.radar.extension.NEUTRAL_DISPLACEMENT_CHANNEL
import se.vadretapp.vadret.radar.extension.toImageBitmap
import se.vadretapp.vadret.radar.precipitation.MOTION_CELL_SIZE
import se.vadretapp.vadret.radar.precipitation.MotionField
import se.vadretapp.vadret.radar.precipitation.PrecipitationMotion
import se.vadretapp.vadret.radar.precipitation.PrecipitationOverlay
import se.vadretapp.vadret.radar.projection.ECHO_LEVEL_COUNT
import se.vadretapp.vadret.radar.projection.PrecipitationColorScale

private const val CHANNEL_MAXIMUM = 255f
private const val NO_PROGRESS = 0f
private const val PALETTE_HEIGHT = 1
private const val STILL_CELLS = 1
private const val NO_DISPLACEMENT = 0f
private const val RASTER_ORIGIN = 0f
private const val SWITCH_PROGRESS = 0.5f

private const val PRECIPITATION_SHADER = """
uniform shader previousEchoes;
uniform shader nextEchoes;
uniform shader motion;
uniform shader palette;
uniform float progress;
uniform float motionCellSize;
uniform float channelMaximum;
uniform float neutralChannel;
uniform float displacementPerStep;
uniform float paletteMaximum;
uniform float switchProgress;

half4 main(float2 position) {
    float2 channels = float2(motion.eval(position / motionCellSize).rg) * channelMaximum;
    float2 displacement = (channels - neutralChannel) * displacementPerStep;
    float echo = 0.0;
    if (progress < switchProgress) {
        echo = previousEchoes.eval(position - displacement * progress).a;
    } else {
        echo = nextEchoes.eval(position + displacement * (1.0 - progress)).a;
    }
    return palette.eval(float2(echo * paletteMaximum + 0.5, 0.5));
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class PrecipitationShaderDrawer {

    private val runtimeShader = RuntimeShader(PRECIPITATION_SHADER)
    private val paint = Paint()
    private val stillMotion: Bitmap = MotionField(
        columns = STILL_CELLS,
        rows = STILL_CELLS,
        eastward = floatArrayOf(NO_DISPLACEMENT),
        southward = floatArrayOf(NO_DISPLACEMENT)
    ).toImageBitmap().asAndroidBitmap()
    private var shownEchoes: Bitmap? = null
    private var shownNextEchoes: Bitmap? = null
    private var shownMotion: Bitmap? = null

    init {
        val palette: IntArray = PrecipitationColorScale.palette()
        runtimeShader.setInputShader(
            "palette",
            bitmapShader(
                bitmap = Bitmap.createBitmap(
                    palette,
                    palette.size,
                    PALETTE_HEIGHT,
                    Bitmap.Config.ARGB_8888
                ),
                tileMode = Shader.TileMode.CLAMP,
                filterMode = BitmapShader.FILTER_MODE_NEAREST
            )
        )
        runtimeShader.setFloatUniform("motionCellSize", MOTION_CELL_SIZE.toFloat())
        runtimeShader.setFloatUniform("channelMaximum", CHANNEL_MAXIMUM)
        runtimeShader.setFloatUniform("neutralChannel", NEUTRAL_DISPLACEMENT_CHANNEL.toFloat())
        runtimeShader.setFloatUniform("displacementPerStep", DISPLACEMENT_PER_CHANNEL_STEP)
        runtimeShader.setFloatUniform("paletteMaximum", (ECHO_LEVEL_COUNT - 1).toFloat())
        runtimeShader.setFloatUniform("switchProgress", SWITCH_PROGRESS)
        paint.shader = runtimeShader
    }

    fun draw(
        canvas: Canvas,
        projection: Projection,
        echoesOverlay: PrecipitationOverlay.Echoes,
        progress: Float
    ) {
        val echoes: Bitmap = echoesOverlay.echoes.asAndroidBitmap()
        val motion: PrecipitationMotion? = echoesOverlay.motion
        bindInputs(
            echoes = echoes,
            nextEchoes = motion?.nextEchoes?.asAndroidBitmap() ?: echoes,
            motionField = motion?.field?.asAndroidBitmap() ?: stillMotion
        )
        runtimeShader.setFloatUniform(
            "progress",
            if (motion == null) {
                NO_PROGRESS
            } else {
                progress
            }
        )
        val rasterProjection: RasterProjection = projection.toRasterProjection(
            rasterWidth = echoes.width,
            rasterHeight = echoes.height
        )
        val left: Float = rasterProjection.screenX(rasterX = RASTER_ORIGIN)
        val top: Float = rasterProjection.screenY(rasterY = RASTER_ORIGIN)
        val width: Float = echoes.width.toFloat()
        val height: Float = echoes.height.toFloat()
        canvas.withTranslation(
            x = left,
            y = top
        ) {
            scale(
                (rasterProjection.screenX(rasterX = width) - left) / width,
                (rasterProjection.screenY(rasterY = height) - top) / height
            )
            drawRect(
                RASTER_ORIGIN,
                RASTER_ORIGIN,
                width,
                height,
                paint
            )
        }
    }

    private fun bindInputs(
        echoes: Bitmap,
        nextEchoes: Bitmap,
        motionField: Bitmap
    ) {
        if (echoes !== shownEchoes) {
            runtimeShader.setInputBuffer("previousEchoes", echoShader(echoes = echoes))
            shownEchoes = echoes
        }
        if (nextEchoes !== shownNextEchoes) {
            runtimeShader.setInputBuffer("nextEchoes", echoShader(echoes = nextEchoes))
            shownNextEchoes = nextEchoes
        }
        if (motionField !== shownMotion) {
            runtimeShader.setInputBuffer(
                "motion",
                bitmapShader(
                    bitmap = motionField,
                    tileMode = Shader.TileMode.CLAMP,
                    filterMode = BitmapShader.FILTER_MODE_LINEAR
                )
            )
            shownMotion = motionField
        }
    }

    private fun echoShader(echoes: Bitmap): BitmapShader =
        bitmapShader(
            bitmap = echoes,
            tileMode = Shader.TileMode.DECAL,
            filterMode = BitmapShader.FILTER_MODE_LINEAR
        )

    private fun bitmapShader(
        bitmap: Bitmap,
        tileMode: Shader.TileMode,
        filterMode: Int
    ): BitmapShader {
        val bitmapShader = BitmapShader(
            bitmap,
            tileMode,
            tileMode
        )
        bitmapShader.filterMode = filterMode
        return bitmapShader
    }
}