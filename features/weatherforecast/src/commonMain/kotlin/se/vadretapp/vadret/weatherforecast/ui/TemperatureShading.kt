package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import se.vadretapp.vadret.ui.VadretColors
import se.vadretapp.vadret.weatherforecast.model.TemperatureShade
import se.vadretapp.vadret.weatherforecast.model.TemperatureShadeStop

private const val FULL_LEVEL = 1f

internal fun ImmutableList<TemperatureShadeStop>.shading(
    lineTopPixels: Float,
    lineRangePixels: Float,
    colors: VadretColors
): Brush {
    val warmestY: Float = first().lineY(
        lineTopPixels = lineTopPixels,
        lineRangePixels = lineRangePixels
    )
    val coldestY: Float = last().lineY(
        lineTopPixels = lineTopPixels,
        lineRangePixels = lineRangePixels
    )
    return Brush.verticalGradient(
        colorStops = colorStops(colors = colors),
        startY = warmestY,
        endY = coldestY
    )
}

internal fun ImmutableList<TemperatureShadeStop>.legendShading(
    lineLengthPixels: Float,
    colors: VadretColors
): Brush =
    Brush.horizontalGradient(
        colorStops = colorStops(colors = colors),
        startX = first().level * lineLengthPixels,
        endX = last().level * lineLengthPixels
    )

private fun ImmutableList<TemperatureShadeStop>.colorStops(colors: VadretColors): Array<Pair<Float, Color>> {
    val warmestLevel: Float = first().level
    val coldestLevel: Float = last().level
    return map { shadeStop: TemperatureShadeStop ->
        (shadeStop.level - warmestLevel) / (coldestLevel - warmestLevel) to shadeStop.shade.color(colors = colors)
    }.toTypedArray()
}

private fun TemperatureShadeStop.lineY(
    lineTopPixels: Float,
    lineRangePixels: Float
): Float = lineTopPixels + (FULL_LEVEL - level) * lineRangePixels

private fun TemperatureShade.color(colors: VadretColors): Color =
    when (this) {
        TemperatureShade.WARM -> colors.temperatureWarm
        TemperatureShade.ZERO -> colors.temperatureZero
        TemperatureShade.MINUS_FIVE -> colors.temperatureMinusFive
        TemperatureShade.MINUS_TEN -> colors.temperatureMinusTen
        TemperatureShade.MINUS_FIFTEEN -> colors.temperatureMinusFifteen
        TemperatureShade.MINUS_TWENTY -> colors.temperatureMinusTwenty
    }