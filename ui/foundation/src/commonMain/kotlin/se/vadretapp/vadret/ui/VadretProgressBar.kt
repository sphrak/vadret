package se.vadretapp.vadret.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

private val barHeight = 4.dp

private const val TRACK_ALPHA = 0.24f
private const val CYCLE_MILLIS = 1800

private const val FIRST_HEAD_DELAY_MILLIS = 0
private const val FIRST_HEAD_DURATION_MILLIS = 750
private const val FIRST_TAIL_DELAY_MILLIS = 333
private const val FIRST_TAIL_DURATION_MILLIS = 850
private const val SECOND_HEAD_DELAY_MILLIS = 1000
private const val SECOND_HEAD_DURATION_MILLIS = 567
private const val SECOND_TAIL_DELAY_MILLIS = 1267
private const val SECOND_TAIL_DURATION_MILLIS = 533

private val firstHeadEasing = CubicBezierEasing(a = 0.2f, b = 0f, c = 0.8f, d = 1f)
private val firstTailEasing = CubicBezierEasing(a = 0.4f, b = 0f, c = 1f, d = 1f)
private val secondHeadEasing = CubicBezierEasing(a = 0f, b = 0f, c = 0.65f, d = 1f)
private val secondTailEasing = CubicBezierEasing(a = 0.1f, b = 0f, c = 0.45f, d = 1f)

@Composable
fun VadretProgressBar(
    modifier: Modifier = Modifier,
    color: Color = VadretTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = TRACK_ALPHA)
) {
    val transition: InfiniteTransition = rememberInfiniteTransition(label = "VadretProgressBar")
    val firstHead: State<Float> = transition.animateSegmentEdge(
        delayMillis = FIRST_HEAD_DELAY_MILLIS,
        durationMillis = FIRST_HEAD_DURATION_MILLIS,
        easing = firstHeadEasing
    )
    val firstTail: State<Float> = transition.animateSegmentEdge(
        delayMillis = FIRST_TAIL_DELAY_MILLIS,
        durationMillis = FIRST_TAIL_DURATION_MILLIS,
        easing = firstTailEasing
    )
    val secondHead: State<Float> = transition.animateSegmentEdge(
        delayMillis = SECOND_HEAD_DELAY_MILLIS,
        durationMillis = SECOND_HEAD_DURATION_MILLIS,
        easing = secondHeadEasing
    )
    val secondTail: State<Float> = transition.animateSegmentEdge(
        delayMillis = SECOND_TAIL_DELAY_MILLIS,
        durationMillis = SECOND_TAIL_DURATION_MILLIS,
        easing = secondTailEasing
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height = barHeight)
    ) {
        drawRect(color = trackColor)
        drawSegment(
            head = firstHead.value,
            tail = firstTail.value,
            color = color
        )
        drawSegment(
            head = secondHead.value,
            tail = secondTail.value,
            color = color
        )
    }
}

@Composable
private fun InfiniteTransition.animateSegmentEdge(
    delayMillis: Int,
    durationMillis: Int,
    easing: Easing
): State<Float> =
    animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                this.durationMillis = CYCLE_MILLIS
                0f at delayMillis using easing
                1f at delayMillis + durationMillis
            }
        ),
        label = "VadretProgressBarSegment"
    )

private fun DrawScope.drawSegment(
    head: Float,
    tail: Float,
    color: Color
) {
    if (head > tail) {
        drawRect(
            color = color,
            topLeft = Offset(
                x = size.width * tail,
                y = 0f
            ),
            size = Size(
                width = size.width * (head - tail),
                height = size.height
            )
        )
    }
}