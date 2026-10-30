package fi.kroon.vadret.radar.wind

import kotlin.math.roundToInt

private const val OPAQUE = 0xFF
private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8

private class ColorStop(
    val windSpeed: Float,
    val red: Int,
    val green: Int,
    val blue: Int
)

private val dayColorStops: List<ColorStop> = listOf(
    ColorStop(windSpeed = 0f, red = 59, green = 70, blue = 160),
    ColorStop(windSpeed = 2f, red = 62, green = 110, blue = 190),
    ColorStop(windSpeed = 4f, red = 52, green = 150, blue = 175),
    ColorStop(windSpeed = 6f, red = 60, green = 170, blue = 120),
    ColorStop(windSpeed = 8f, red = 80, green = 180, blue = 70),
    ColorStop(windSpeed = 11f, red = 190, green = 175, blue = 70),
    ColorStop(windSpeed = 14f, red = 210, green = 130, blue = 70),
    ColorStop(windSpeed = 17f, red = 195, green = 75, blue = 120),
    ColorStop(windSpeed = 20f, red = 150, green = 60, blue = 170),
    ColorStop(windSpeed = 26f, red = 110, green = 55, blue = 160)
)

private val nightColorStops: List<ColorStop> = listOf(
    ColorStop(windSpeed = 0f, red = 14, green = 20, blue = 48),
    ColorStop(windSpeed = 2f, red = 18, green = 32, blue = 80),
    ColorStop(windSpeed = 4f, red = 22, green = 48, blue = 112),
    ColorStop(windSpeed = 6f, red = 26, green = 66, blue = 140),
    ColorStop(windSpeed = 8f, red = 30, green = 86, blue = 162),
    ColorStop(windSpeed = 11f, red = 38, green = 110, blue = 184),
    ColorStop(windSpeed = 14f, red = 50, green = 132, blue = 202),
    ColorStop(windSpeed = 17f, red = 66, green = 152, blue = 216),
    ColorStop(windSpeed = 20f, red = 88, green = 172, blue = 228),
    ColorStop(windSpeed = 26f, red = 122, green = 196, blue = 240)
)

internal object WindColorScale {

    fun argb(
        windSpeed: Float,
        windPalette: WindPalette
    ): Int {
        val colorStops: List<ColorStop> = windPalette.colorStops()
        val upperIndex: Int = colorStops.indexOfFirst { colorStop: ColorStop -> colorStop.windSpeed >= windSpeed }
        return when (upperIndex) {
            -1 -> colorStops.last().argb()
            0 -> colorStops.first().argb()
            else -> {
                val lower: ColorStop = colorStops[upperIndex - 1]
                val upper: ColorStop = colorStops[upperIndex]
                val fraction: Float = (windSpeed - lower.windSpeed) / (upper.windSpeed - lower.windSpeed)
                argb(
                    red = interpolate(from = lower.red, to = upper.red, fraction = fraction),
                    green = interpolate(from = lower.green, to = upper.green, fraction = fraction),
                    blue = interpolate(from = lower.blue, to = upper.blue, fraction = fraction)
                )
            }
        }
    }

    private fun WindPalette.colorStops(): List<ColorStop> =
        when (this) {
            WindPalette.DAY -> dayColorStops
            WindPalette.NIGHT -> nightColorStops
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