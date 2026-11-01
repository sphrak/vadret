package se.vadretapp.vadret.forecastwarning.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import se.vadretapp.vadret.ui.VadretTheme

internal val segmentHeight = 3.dp
private val segmentSpacing = 5.dp
private const val SEGMENT_TRACK_ALPHA = 0.22f
private const val EMPTY = 0f
private const val FULL = 1f

@Composable
internal fun StoryProgress(
    storyCount: Int,
    currentIndex: Int,
    currentProgress: () -> Float,
    modifier: Modifier = Modifier
) {
    val fillColor: Color = VadretTheme.colorScheme.onSurface
    val trackColor: Color = fillColor.copy(alpha = SEGMENT_TRACK_ALPHA)
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = segmentSpacing),
        modifier = modifier.fillMaxWidth()
    ) {
        repeat(times = storyCount) { index: Int ->
            Box(
                modifier = Modifier
                    .weight(weight = 1f)
                    .height(height = segmentHeight)
                    .clip(shape = CircleShape)
                    .background(color = trackColor)
                    .drawBehind {
                        val fraction: Float = when {
                            index < currentIndex -> FULL
                            index == currentIndex -> currentProgress()
                            else -> EMPTY
                        }
                        drawRect(
                            color = fillColor,
                            size = Size(
                                width = size.width * fraction,
                                height = size.height
                            )
                        )
                    }
            )
        }
    }
}