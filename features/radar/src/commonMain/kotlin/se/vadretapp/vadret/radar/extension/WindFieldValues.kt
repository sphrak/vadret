package se.vadretapp.vadret.radar.extension

import se.vadretapp.vadret.radar.wind.WindFieldValues
import kotlin.math.roundToInt

private const val CHANNEL_MASK = 0xFF
private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val BLUE_SHIFT = 0

private val channelShifts: IntArray = intArrayOf(ALPHA_SHIFT, RED_SHIFT, GREEN_SHIFT, BLUE_SHIFT)

internal fun WindFieldValues.towards(
    nextWindFieldValues: WindFieldValues,
    progress: Float
): WindFieldValues {
    if (width != nextWindFieldValues.width || height != nextWindFieldValues.height) {
        return nextWindFieldValues
    }
    return WindFieldValues(
        width = width,
        height = height,
        eastward = eastward.towards(
            nextValues = nextWindFieldValues.eastward,
            progress = progress
        ),
        northward = northward.towards(
            nextValues = nextWindFieldValues.northward,
            progress = progress
        ),
        speeds = speeds.towards(
            nextValues = nextWindFieldValues.speeds,
            progress = progress
        ),
        colors = IntArray(size = colors.size) { pixel: Int ->
            blendColor(
                color = colors[pixel],
                nextColor = nextWindFieldValues.colors[pixel],
                progress = progress
            )
        }
    )
}

private fun FloatArray.towards(
    nextValues: FloatArray,
    progress: Float
): FloatArray = FloatArray(size = size) { pixel: Int -> this[pixel] + (nextValues[pixel] - this[pixel]) * progress }

private fun blendColor(
    color: Int,
    nextColor: Int,
    progress: Float
): Int {
    var blended = 0
    for (shift in channelShifts) {
        val channel: Int = color shr shift and CHANNEL_MASK
        val nextChannel: Int = nextColor shr shift and CHANNEL_MASK
        blended = blended or ((channel + (nextChannel - channel) * progress).roundToInt() shl shift)
    }
    return blended
}