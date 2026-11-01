package se.vadretapp.vadret.radar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import se.vadretapp.vadret.ui.VadretTheme

private val thumbSize = 16.dp
private val thumbBorderWidth = 2.dp
private val thumbElevation = 2.dp
private val trackHeight = 4.dp

@Composable
internal fun RadarSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    val activeColor: Color = VadretTheme.colorScheme.primary
    val inactiveColor: Color = VadretTheme.colorScheme.surfaceContainerHighest
    val sliderState: SliderState = remember(valueRange) {
        SliderState(
            value = value,
            trackRange = valueRange
        )
    }
    sliderState.value = value
    Slider(
        state = sliderState,
        onValueChange = onValueChange,
        thumb = {
            Surface(
                shape = CircleShape,
                color = VadretTheme.colorScheme.surface,
                shadowElevation = thumbElevation,
                modifier = Modifier.size(size = thumbSize)
            ) {
                Box(
                    modifier = Modifier
                        .padding(all = thumbBorderWidth)
                        .background(
                            color = activeColor,
                            shape = CircleShape
                        )
                )
            }
        },
        track = { state: SliderState ->
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = trackHeight)
            ) {
                val cornerRadius = CornerRadius(x = size.height / 2f, y = size.height / 2f)
                drawRoundRect(
                    color = inactiveColor,
                    cornerRadius = cornerRadius
                )
                drawRoundRect(
                    color = activeColor,
                    size = Size(
                        width = size.width * state.fraction(),
                        height = size.height
                    ),
                    cornerRadius = cornerRadius
                )
            }
        },
        modifier = modifier
    )
}

private fun SliderState.fraction(): Float {
    val span: Float = trackRange.endInclusive - trackRange.start
    return if (span > 0f) {
        ((value - trackRange.start) / span).coerceIn(
            minimumValue = 0f,
            maximumValue = 1f
        )
    } else {
        0f
    }
}