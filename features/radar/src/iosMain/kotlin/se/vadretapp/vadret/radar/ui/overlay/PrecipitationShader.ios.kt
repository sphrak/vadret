package se.vadretapp.vadret.radar.ui.overlay

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.asComposeShader
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.SamplingMode
import se.vadretapp.vadret.radar.image.argbImageBitmap
import se.vadretapp.vadret.radar.projection.PrecipitationColorScale

private const val PALETTE_HEIGHT = 1

internal actual fun precipitationShaderOrNull(): PrecipitationShader? = DefaultPrecipitationShader()

internal class DefaultPrecipitationShader : PrecipitationShader {

    private val runtimeShaderBuilder = RuntimeShaderBuilder(effect = RuntimeEffect.makeForShader(sksl = PRECIPITATION_SHADER))
    private var shownEchoes: ImageBitmap? = null
    private var shownNextEchoes: ImageBitmap? = null
    private var shownMotionField: ImageBitmap? = null

    init {
        val palette: IntArray = PrecipitationColorScale.palette()
        runtimeShaderBuilder.child(
            name = PALETTE_INPUT,
            shader = argbImageBitmap(
                pixels = palette,
                width = palette.size,
                height = PALETTE_HEIGHT
            ).asSkiaBitmap().makeShader(
                tmx = FilterTileMode.CLAMP,
                tmy = FilterTileMode.CLAMP,
                sampling = SamplingMode.DEFAULT,
                localMatrix = null
            )
        )
        runtimeShaderBuilder.uniform(name = MOTION_CELL_SIZE_UNIFORM, value = MOTION_CELL_SIZE_VALUE)
        runtimeShaderBuilder.uniform(name = CHANNEL_MAXIMUM_UNIFORM, value = CHANNEL_MAXIMUM)
        runtimeShaderBuilder.uniform(name = NEUTRAL_CHANNEL_UNIFORM, value = NEUTRAL_CHANNEL_VALUE)
        runtimeShaderBuilder.uniform(name = DISPLACEMENT_PER_STEP_UNIFORM, value = DISPLACEMENT_PER_STEP_VALUE)
        runtimeShaderBuilder.uniform(name = PALETTE_MAXIMUM_UNIFORM, value = PALETTE_MAXIMUM_VALUE)
        runtimeShaderBuilder.uniform(name = SWITCH_PROGRESS_UNIFORM, value = SWITCH_PROGRESS)
    }

    override fun shader(
        echoes: ImageBitmap,
        nextEchoes: ImageBitmap,
        motionField: ImageBitmap,
        progress: Float
    ): Shader {
        if (echoes !== shownEchoes) {
            runtimeShaderBuilder.child(name = PREVIOUS_ECHOES_INPUT, shader = echoShader(echoes = echoes))
            shownEchoes = echoes
        }
        if (nextEchoes !== shownNextEchoes) {
            runtimeShaderBuilder.child(name = NEXT_ECHOES_INPUT, shader = echoShader(echoes = nextEchoes))
            shownNextEchoes = nextEchoes
        }
        if (motionField !== shownMotionField) {
            runtimeShaderBuilder.child(
                name = MOTION_INPUT,
                shader = motionField.asSkiaBitmap().makeShader(
                    tmx = FilterTileMode.CLAMP,
                    tmy = FilterTileMode.CLAMP,
                    sampling = SamplingMode.LINEAR,
                    localMatrix = null
                )
            )
            shownMotionField = motionField
        }
        runtimeShaderBuilder.uniform(name = PROGRESS_UNIFORM, value = progress)
        return runtimeShaderBuilder.makeShader().asComposeShader()
    }

    private fun echoShader(echoes: ImageBitmap): org.jetbrains.skia.Shader =
        echoes.asSkiaBitmap().makeShader(
            tmx = FilterTileMode.DECAL,
            tmy = FilterTileMode.DECAL,
            sampling = SamplingMode.LINEAR,
            localMatrix = null
        )
}