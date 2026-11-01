package se.vadretapp.vadret.radar.projection

import kotlin.math.ceil
import kotlin.math.roundToInt

internal const val ECHO_LEVEL_COUNT = 256

private const val REFLECTIVITY_GAIN = 0.4f
private const val REFLECTIVITY_OFFSET = -30f
private const val TRANSPARENT = 0
private const val OPAQUE = 0xFF
private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8

private class ColorStop(
    val reflectivity: Float,
    val red: Int,
    val green: Int,
    val blue: Int
)

private val colorStops: List<ColorStop> = listOf(
    ColorStop(reflectivity = 5f, red = 156, green = 201, blue = 255),
    ColorStop(reflectivity = 15f, red = 47, green = 111, blue = 224),
    ColorStop(reflectivity = 25f, red = 45, green = 179, blue = 74),
    ColorStop(reflectivity = 35f, red = 242, green = 207, blue = 46),
    ColorStop(reflectivity = 45f, red = 236, green = 90, blue = 42)
)

internal object PrecipitationColorScale {

    val firstVisibleEchoLevel: Int = ceil((colorStops.first().reflectivity - REFLECTIVITY_OFFSET) / REFLECTIVITY_GAIN).toInt()

    fun palette(): IntArray = IntArray(size = ECHO_LEVEL_COUNT) { echoLevel: Int -> argb(reflectivity = echoLevel * REFLECTIVITY_GAIN + REFLECTIVITY_OFFSET) }

    fun argb(reflectivity: Float): Int {
        if (reflectivity.isNaN() || reflectivity < colorStops.first().reflectivity) {
            return TRANSPARENT
        }
        val upperIndex: Int = colorStops.indexOfFirst { colorStop: ColorStop -> colorStop.reflectivity >= reflectivity }
        return when (upperIndex) {
            -1 -> colorStops.last().argb()
            0 -> colorStops.first().argb()
            else -> {
                val lower: ColorStop = colorStops[upperIndex - 1]
                val upper: ColorStop = colorStops[upperIndex]
                val fraction: Float = (reflectivity - lower.reflectivity) / (upper.reflectivity - lower.reflectivity)
                argb(
                    red = interpolate(from = lower.red, to = upper.red, fraction = fraction),
                    green = interpolate(from = lower.green, to = upper.green, fraction = fraction),
                    blue = interpolate(from = lower.blue, to = upper.blue, fraction = fraction)
                )
            }
        }
    }

    private fun ColorStop.argb(): Int =
        argb(
            red = red,
            green = green,
            blue = blue
        )

    private fun argb(
        red: Int,
        green: Int,
        blue: Int
    ): Int = (OPAQUE shl ALPHA_SHIFT) or (red shl RED_SHIFT) or (green shl GREEN_SHIFT) or blue

    private fun interpolate(
        from: Int,
        to: Int,
        fraction: Float
    ): Int = (from + (to - from) * fraction).roundToInt()
}