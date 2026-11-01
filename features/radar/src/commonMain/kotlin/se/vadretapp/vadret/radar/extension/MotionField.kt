package se.vadretapp.vadret.radar.extension

import androidx.compose.ui.graphics.ImageBitmap
import se.vadretapp.vadret.radar.image.argbImageBitmap
import se.vadretapp.vadret.radar.precipitation.MAXIMUM_ECHO_DISPLACEMENT
import se.vadretapp.vadret.radar.precipitation.MotionField
import kotlin.math.roundToInt

private const val DISPLACEMENT_CHANNEL_RANGE = 127f
private const val CHANNEL_MINIMUM = 0
private const val CHANNEL_MAXIMUM = 0xFF
private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val OPAQUE = CHANNEL_MAXIMUM shl ALPHA_SHIFT

internal const val NEUTRAL_DISPLACEMENT_CHANNEL = 128
internal const val DISPLACEMENT_PER_CHANNEL_STEP = MAXIMUM_ECHO_DISPLACEMENT / DISPLACEMENT_CHANNEL_RANGE

internal fun MotionField.toImageBitmap(): ImageBitmap =
    argbImageBitmap(
        pixels = IntArray(size = columns * rows) { cellIndex: Int ->
            OPAQUE or
                (eastward[cellIndex].toChannel() shl RED_SHIFT) or
                (southward[cellIndex].toChannel() shl GREEN_SHIFT)
        },
        width = columns,
        height = rows
    )

private fun Float.toChannel(): Int =
    (NEUTRAL_DISPLACEMENT_CHANNEL + this / DISPLACEMENT_PER_CHANNEL_STEP)
        .roundToInt()
        .coerceIn(
            minimumValue = CHANNEL_MINIMUM,
            maximumValue = CHANNEL_MAXIMUM
        )