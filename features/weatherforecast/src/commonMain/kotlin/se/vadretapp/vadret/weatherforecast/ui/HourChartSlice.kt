package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import se.vadretapp.vadret.weatherforecast.model.ComingHour

private const val FIRST_INDEX = 0
private const val FULL_LEVEL = 1f
private const val HALF_DIVISOR = 2f
private const val SMOOTHING_DIVISOR = 6f
private const val AREA_ALPHA = 0.12f
private const val NO_ROTATION = 0f
private const val NO_WIDTH = 0f
private const val SINGLE_LINE = 1

private val lineWidth = 2.5.dp
private val labelCenterAbovePoint = 11.dp
private val arrowSize = 12.dp
private val arrowSpacing = 1.dp

private class SlicePoints(
    private val hours: ImmutableList<ComingHour>,
    private val index: Int,
    private val level: (ComingHour) -> Float,
    private val columnWidth: Float,
    private val lineTop: Float,
    private val lineRange: Float
) {

    val firstIndex: Int = index - 1
    val lastIndex: Int = index + 1

    fun x(pointIndex: Int): Float = (pointIndex - index) * columnWidth + columnWidth / HALF_DIVISOR

    fun y(pointIndex: Int): Float = lineTop + (FULL_LEVEL - level(hours[pointIndex.shown()])) * lineRange

    private fun Int.shown(): Int =
        coerceIn(
            minimumValue = FIRST_INDEX,
            maximumValue = hours.lastIndex
        )
}

@Composable
internal fun HourChartSlice(
    hours: ImmutableList<ComingHour>,
    index: Int,
    level: (ComingHour) -> Float,
    lineColor: Color,
    lineTop: Dp,
    lineRange: Dp,
    labelText: String,
    labelStyle: TextStyle,
    labelColor: Color,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier,
    arrow: Painter? = null,
    arrowRotation: Float = NO_ROTATION
) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .drawWithCache {
                val slicePoints = SlicePoints(
                    hours = hours,
                    index = index,
                    level = level,
                    columnWidth = size.width,
                    lineTop = lineTop.toPx(),
                    lineRange = lineRange.toPx()
                )
                val line: Path = slicePoints.line()
                val area: Path = slicePoints.area(
                    line = line,
                    bottom = size.height
                )
                val areaColor: Color = lineColor.copy(alpha = AREA_ALPHA)
                val stroke = Stroke(
                    width = lineWidth.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                val labelLayout: TextLayoutResult = textMeasurer.measure(
                    text = labelText,
                    style = labelStyle,
                    maxLines = SINGLE_LINE
                )
                val arrowSizePx: Float = arrowSize.toPx()
                val arrowWidth: Float = if (arrow == null) {
                    NO_WIDTH
                } else {
                    arrowSizePx + arrowSpacing.toPx()
                }
                val labelLeft: Float = (size.width - arrowWidth - labelLayout.size.width) / HALF_DIVISOR
                val labelCenterY: Float = slicePoints.y(pointIndex = index) - labelCenterAbovePoint.toPx()
                val arrowColorFilter: ColorFilter = ColorFilter.tint(color = lineColor)
                onDrawBehind {
                    clipRect {
                        drawPath(
                            path = area,
                            color = areaColor
                        )
                        drawPath(
                            path = line,
                            color = lineColor,
                            style = stroke
                        )
                    }
                    if (arrow != null) {
                        translate(
                            left = labelLeft,
                            top = labelCenterY - arrowSizePx / HALF_DIVISOR
                        ) {
                            rotate(
                                degrees = arrowRotation,
                                pivot = Offset(
                                    x = arrowSizePx / HALF_DIVISOR,
                                    y = arrowSizePx / HALF_DIVISOR
                                )
                            ) {
                                with(receiver = arrow) {
                                    draw(
                                        size = Size(
                                            width = arrowSizePx,
                                            height = arrowSizePx
                                        ),
                                        colorFilter = arrowColorFilter
                                    )
                                }
                            }
                        }
                    }
                    drawText(
                        textLayoutResult = labelLayout,
                        color = labelColor,
                        topLeft = Offset(
                            x = labelLeft + arrowWidth,
                            y = labelCenterY - labelLayout.size.height / HALF_DIVISOR
                        )
                    )
                }
            }
    )
}

private fun SlicePoints.line(): Path {
    val path = Path()
    path.moveTo(
        x = x(pointIndex = firstIndex),
        y = y(pointIndex = firstIndex)
    )
    for (pointIndex in firstIndex until lastIndex) {
        path.cubicTo(
            x1 = x(pointIndex = pointIndex) + (x(pointIndex = pointIndex + 1) - x(pointIndex = pointIndex - 1)) / SMOOTHING_DIVISOR,
            y1 = y(pointIndex = pointIndex) + (y(pointIndex = pointIndex + 1) - y(pointIndex = pointIndex - 1)) / SMOOTHING_DIVISOR,
            x2 = x(pointIndex = pointIndex + 1) - (x(pointIndex = pointIndex + 2) - x(pointIndex = pointIndex)) / SMOOTHING_DIVISOR,
            y2 = y(pointIndex = pointIndex + 1) - (y(pointIndex = pointIndex + 2) - y(pointIndex = pointIndex)) / SMOOTHING_DIVISOR,
            x3 = x(pointIndex = pointIndex + 1),
            y3 = y(pointIndex = pointIndex + 1)
        )
    }
    return path
}

private fun SlicePoints.area(
    line: Path,
    bottom: Float
): Path {
    val path = Path()
    path.addPath(path = line)
    path.lineTo(
        x = x(pointIndex = lastIndex),
        y = bottom
    )
    path.lineTo(
        x = x(pointIndex = firstIndex),
        y = bottom
    )
    path.close()
    return path
}