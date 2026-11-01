package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import se.vadretapp.vadret.ui.VadretColors
import se.vadretapp.vadret.ui.VadretTheme
import kotlin.math.max
import kotlin.math.min

private val barHeight = 5.dp
private const val HALF = 0.5f
private const val COLD_STOP = 0f
private const val COOL_STOP = 0.4f
private const val MILD_STOP = 0.7f
private const val WARM_STOP = 1f
private const val TRACK_START = 0f
private const val TRACK_TOP = 0f

@Composable
internal fun TemperatureRangeBar(
    rangeStart: Float,
    rangeEnd: Float,
    modifier: Modifier = Modifier
) {
    val colors: VadretColors = VadretTheme.colors
    val trackColor: Color = colors.divider
    val colorStops: Array<Pair<Float, Color>> = remember(colors) {
        arrayOf(
            COLD_STOP to colors.temperatureCold,
            COOL_STOP to colors.temperatureCool,
            MILD_STOP to colors.temperatureMild,
            WARM_STOP to colors.temperatureWarm
        )
    }
    Canvas(modifier = modifier.height(height = barHeight)) {
        val cornerRadius = CornerRadius(
            x = size.height * HALF,
            y = size.height * HALF
        )
        drawRoundRect(
            color = trackColor,
            cornerRadius = cornerRadius
        )
        val rangeWidth: Float = max(
            a = size.width * (rangeEnd - rangeStart),
            b = size.height
        )
        val rangeLeft: Float = min(
            a = size.width * rangeStart,
            b = size.width - rangeWidth
        )
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colorStops = colorStops,
                startX = TRACK_START,
                endX = size.width
            ),
            topLeft = Offset(
                x = rangeLeft,
                y = TRACK_TOP
            ),
            size = Size(
                width = rangeWidth,
                height = size.height
            ),
            cornerRadius = cornerRadius
        )
    }
}