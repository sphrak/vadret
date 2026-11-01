package se.vadretapp.vadret.radar.ui.overlay

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shader
import se.vadretapp.vadret.radar.extension.DISPLACEMENT_PER_CHANNEL_STEP
import se.vadretapp.vadret.radar.extension.NEUTRAL_DISPLACEMENT_CHANNEL
import se.vadretapp.vadret.radar.precipitation.MOTION_CELL_SIZE
import se.vadretapp.vadret.radar.projection.ECHO_LEVEL_COUNT

internal const val PREVIOUS_ECHOES_INPUT = "previousEchoes"
internal const val NEXT_ECHOES_INPUT = "nextEchoes"
internal const val MOTION_INPUT = "motion"
internal const val PALETTE_INPUT = "palette"
internal const val PROGRESS_UNIFORM = "progress"
internal const val MOTION_CELL_SIZE_UNIFORM = "motionCellSize"
internal const val CHANNEL_MAXIMUM_UNIFORM = "channelMaximum"
internal const val NEUTRAL_CHANNEL_UNIFORM = "neutralChannel"
internal const val DISPLACEMENT_PER_STEP_UNIFORM = "displacementPerStep"
internal const val PALETTE_MAXIMUM_UNIFORM = "paletteMaximum"
internal const val SWITCH_PROGRESS_UNIFORM = "switchProgress"

internal const val CHANNEL_MAXIMUM = 255f
internal const val SWITCH_PROGRESS = 0.5f
internal const val MOTION_CELL_SIZE_VALUE: Float = MOTION_CELL_SIZE.toFloat()
internal const val NEUTRAL_CHANNEL_VALUE: Float = NEUTRAL_DISPLACEMENT_CHANNEL.toFloat()
internal const val DISPLACEMENT_PER_STEP_VALUE: Float = DISPLACEMENT_PER_CHANNEL_STEP
internal const val PALETTE_MAXIMUM_VALUE: Float = (ECHO_LEVEL_COUNT - 1).toFloat()

internal const val PRECIPITATION_SHADER = """
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

internal interface PrecipitationShader {
    fun shader(
        echoes: ImageBitmap,
        nextEchoes: ImageBitmap,
        motionField: ImageBitmap,
        progress: Float
    ): Shader
}

internal expect fun precipitationShaderOrNull(): PrecipitationShader?