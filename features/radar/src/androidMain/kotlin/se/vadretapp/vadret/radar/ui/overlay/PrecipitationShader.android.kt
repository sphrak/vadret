package se.vadretapp.vadret.radar.ui.overlay

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.asAndroidBitmap
import se.vadretapp.vadret.radar.image.argbImageBitmap
import se.vadretapp.vadret.radar.projection.PrecipitationColorScale

private const val PALETTE_HEIGHT = 1

internal actual fun precipitationShaderOrNull(): PrecipitationShader? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        DefaultPrecipitationShader()
    } else {
        null
    }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class DefaultPrecipitationShader : PrecipitationShader {

    private val runtimeShader = RuntimeShader(PRECIPITATION_SHADER)
    private var shownEchoes: ImageBitmap? = null
    private var shownNextEchoes: ImageBitmap? = null
    private var shownMotionField: ImageBitmap? = null

    init {
        val palette: IntArray = PrecipitationColorScale.palette()
        runtimeShader.setInputShader(
            PALETTE_INPUT,
            bitmapShader(
                bitmap = argbImageBitmap(
                    pixels = palette,
                    width = palette.size,
                    height = PALETTE_HEIGHT
                ).asAndroidBitmap(),
                tileMode = android.graphics.Shader.TileMode.CLAMP,
                filterMode = BitmapShader.FILTER_MODE_NEAREST
            )
        )
        runtimeShader.setFloatUniform(MOTION_CELL_SIZE_UNIFORM, MOTION_CELL_SIZE_VALUE)
        runtimeShader.setFloatUniform(CHANNEL_MAXIMUM_UNIFORM, CHANNEL_MAXIMUM)
        runtimeShader.setFloatUniform(NEUTRAL_CHANNEL_UNIFORM, NEUTRAL_CHANNEL_VALUE)
        runtimeShader.setFloatUniform(DISPLACEMENT_PER_STEP_UNIFORM, DISPLACEMENT_PER_STEP_VALUE)
        runtimeShader.setFloatUniform(PALETTE_MAXIMUM_UNIFORM, PALETTE_MAXIMUM_VALUE)
        runtimeShader.setFloatUniform(SWITCH_PROGRESS_UNIFORM, SWITCH_PROGRESS)
    }

    override fun shader(
        echoes: ImageBitmap,
        nextEchoes: ImageBitmap,
        motionField: ImageBitmap,
        progress: Float
    ): Shader {
        if (echoes !== shownEchoes) {
            runtimeShader.setInputBuffer(PREVIOUS_ECHOES_INPUT, echoShader(echoes = echoes))
            shownEchoes = echoes
        }
        if (nextEchoes !== shownNextEchoes) {
            runtimeShader.setInputBuffer(NEXT_ECHOES_INPUT, echoShader(echoes = nextEchoes))
            shownNextEchoes = nextEchoes
        }
        if (motionField !== shownMotionField) {
            runtimeShader.setInputBuffer(
                MOTION_INPUT,
                bitmapShader(
                    bitmap = motionField.asAndroidBitmap(),
                    tileMode = android.graphics.Shader.TileMode.CLAMP,
                    filterMode = BitmapShader.FILTER_MODE_LINEAR
                )
            )
            shownMotionField = motionField
        }
        runtimeShader.setFloatUniform(PROGRESS_UNIFORM, progress)
        return runtimeShader
    }

    private fun echoShader(echoes: ImageBitmap): BitmapShader =
        bitmapShader(
            bitmap = echoes.asAndroidBitmap(),
            tileMode = android.graphics.Shader.TileMode.DECAL,
            filterMode = BitmapShader.FILTER_MODE_LINEAR
        )

    private fun bitmapShader(
        bitmap: Bitmap,
        tileMode: android.graphics.Shader.TileMode,
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