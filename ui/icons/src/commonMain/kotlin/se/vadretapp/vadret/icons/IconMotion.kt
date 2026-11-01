package se.vadretapp.vadret.icons

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val FULL_TURN_RADIANS = 2 * PI
private const val DRIFT_DISTANCE = 3f
private const val FALL_DISTANCE = 10f
private const val FLAKE_DISTANCE = 8f
private const val FLAKE_SWAY = 1.5f
private const val FOG_DISTANCE = 3f
private const val FOG_LOWEST_ALPHA = 0.55f
private const val SLANT = -0.42f
private const val FADE_IN_FRACTION = 0.15f
private const val FADE_OUT_FRACTION = 0.3f
private const val FIRST_FLASH_START = 0.8f
private const val FIRST_FLASH_END = 0.84f
private const val SECOND_FLASH_START = 0.88f
private const val SECOND_FLASH_END = 0.92f
private const val FIRST_FLASH_ALPHA = 0.25f
private const val SECOND_FLASH_ALPHA = 0.35f
private const val HALF = 0.5f
private const val EASE_IN_OUT_FIRST_X = 0.42f
private const val EASE_IN_OUT_FIRST_Y = 0f
private const val EASE_IN_OUT_SECOND_X = 0.58f
private const val EASE_IN_OUT_SECOND_Y = 1f
private const val FULL_ALPHA = 1f
private const val NO_ALPHA = 0f

private val easeInOut: Easing = CubicBezierEasing(
    a = EASE_IN_OUT_FIRST_X,
    b = EASE_IN_OUT_FIRST_Y,
    c = EASE_IN_OUT_SECOND_X,
    d = EASE_IN_OUT_SECOND_Y
)

internal enum class IconMotionKind {
    DRIFT,
    FALL,
    SLANTED_FALL,
    FLAKE,
    SLANTED_FLAKE,
    FOG,
    BOLT
}

internal enum class IconEasing(
    val curve: Easing
) {
    LINEAR(curve = LinearEasing),
    EASE_IN_OUT(curve = easeInOut)
}

internal class IconMotion(
    val kind: IconMotionKind,
    val durationMilliseconds: Int,
    val delayMilliseconds: Int,
    val easing: IconEasing
)

internal class IconPose(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val alpha: Float = FULL_ALPHA
)

internal val restingPose = IconPose()

internal fun IconMotion.poseAt(timeMilliseconds: Long): IconPose {
    val progress: Float = (timeMilliseconds - delayMilliseconds).mod(other = durationMilliseconds.toLong()).toFloat() / durationMilliseconds
    val eased: Float = easing.curve.transform(fraction = progress)
    val wave: Double = FULL_TURN_RADIANS * eased
    return when (kind) {
        IconMotionKind.DRIFT -> IconPose(offsetX = DRIFT_DISTANCE * (HALF - HALF * cos(x = wave).toFloat()))
        IconMotionKind.FALL -> IconPose(
            offsetY = FALL_DISTANCE * eased,
            alpha = fadeInAndOut(progress = eased)
        )
        IconMotionKind.SLANTED_FALL -> IconPose(
            offsetX = SLANT * FALL_DISTANCE * eased,
            offsetY = FALL_DISTANCE * eased,
            alpha = fadeInAndOut(progress = eased)
        )
        IconMotionKind.FLAKE -> IconPose(
            offsetX = FLAKE_SWAY * sin(x = wave).toFloat(),
            offsetY = FLAKE_DISTANCE * eased,
            alpha = fadeInAndOut(progress = eased)
        )
        IconMotionKind.SLANTED_FLAKE -> IconPose(
            offsetX = SLANT * FLAKE_DISTANCE * eased + FLAKE_SWAY * sin(x = wave).toFloat(),
            offsetY = FLAKE_DISTANCE * eased,
            alpha = fadeInAndOut(progress = eased)
        )
        IconMotionKind.FOG -> IconPose(
            offsetX = FOG_DISTANCE * sin(x = wave).toFloat(),
            alpha = FOG_LOWEST_ALPHA + (FULL_ALPHA - FOG_LOWEST_ALPHA) * (HALF + HALF * cos(x = wave).toFloat())
        )
        IconMotionKind.BOLT -> IconPose(alpha = boltAlpha(progress = eased))
    }
}

private fun fadeInAndOut(progress: Float): Float {
    val fadeIn: Float = (progress / FADE_IN_FRACTION).coerceAtMost(maximumValue = FULL_ALPHA)
    val fadeOut: Float = ((FULL_ALPHA - progress) / FADE_OUT_FRACTION).coerceAtMost(maximumValue = FULL_ALPHA)
    return (fadeIn * fadeOut).coerceAtLeast(minimumValue = NO_ALPHA)
}

private fun boltAlpha(progress: Float): Float =
    when (progress) {
        in FIRST_FLASH_START..FIRST_FLASH_END -> FIRST_FLASH_ALPHA
        in SECOND_FLASH_START..SECOND_FLASH_END -> SECOND_FLASH_ALPHA
        else -> FULL_ALPHA
    }