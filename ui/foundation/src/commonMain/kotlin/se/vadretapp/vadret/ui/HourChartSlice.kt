package se.vadretapp.vadret.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
import se.vadretapp.vadret.ui.model.HourChartLinePlacement

private const val FIRST_INDEX = 0
private const val FULL_LEVEL = 1f
private const val HALF_DIVISOR = 2f
private const val SMOOTHING_DIVISOR = 6f
private const val AREA_ALPHA = 0.12f
private const val NO_ROTATION = 0f
private const val NO_WIDTH = 0f
private const val SINGLE_LINE = 1

private val labelCenterAbovePoint = 11.dp
private val labelCenterBelowPoint = 12.dp
private val arrowSize = 12.dp
private val arrowSpacing = 1.dp

private class SlicePoints<T>(
    private val hours: ImmutableList<T>,
    private val index: Int,
    private val level: (T) -> Float,
    private val columnWidth: Float,
    private val lineTop: Float,
    private val lineRange: Float
) {

    val firstIndex: Int = (index - 1).shown()
    val lastIndex: Int = (index + 1).shown()

    fun x(pointIndex: Int): Float = (pointIndex - index) * columnWidth + columnWidth / HALF_DIVISOR

    fun y(pointIndex: Int): Float = lineTop + (FULL_LEVEL - level(hours[pointIndex.shown()])) * lineRange

    private fun Int.shown(): Int =
        coerceIn(
            minimumValue = FIRST_INDEX,
            maximumValue = hours.lastIndex
        )
}

object HourChartSliceDefaults {
    const val SECONDARY_LINE_ALPHA = 0.45f
    val LineWidth = 2.5.dp
    val SecondaryLineWidth = 2.dp
}

@Composable
fun <T> HourChartSlice(
    hours: ImmutableList<T>,
    index: Int,
    level: (T) -> Float,
    lineColor: Color,
    lineTop: Dp,
    lineRange: Dp,
    labelText: String,
    labelStyle: TextStyle,
    labelColor: Color,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier,
    lineBrush: Brush = SolidColor(value = lineColor),
    secondaryLevel: ((T) -> Float)? = null,
    secondaryPlacement: HourChartLinePlacement = HourChartLinePlacement.BELOW,
    secondaryLabelText: String? = null,
    secondaryLabelStyle: TextStyle = labelStyle,
    secondaryLabelColor: Color = labelColor,
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
                val stroke = Stroke(
                    width = HourChartSliceDefaults.LineWidth.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                val secondaryPoints: SlicePoints<T>? = if (secondaryLevel == null) {
                    null
                } else {
                    SlicePoints(
                        hours = hours,
                        index = index,
                        level = secondaryLevel,
                        columnWidth = size.width,
                        lineTop = lineTop.toPx(),
                        lineRange = lineRange.toPx()
                    )
                }
                val secondaryLine: Path? = secondaryPoints?.line()
                val secondaryArea: Path? = if (secondaryLine == null) {
                    null
                } else {
                    slicePoints.area(
                        line = secondaryLine,
                        bottom = size.height
                    )
                }
                val isSecondaryAbove: Boolean = secondaryArea != null && secondaryPlacement == HourChartLinePlacement.ABOVE
                val upperArea: Path = if (secondaryArea != null && isSecondaryAbove) {
                    secondaryArea
                } else {
                    area
                }
                val lowerArea: Path? = if (isSecondaryAbove) {
                    area
                } else {
                    secondaryArea
                }
                val secondaryStroke = Stroke(
                    width = HourChartSliceDefaults.SecondaryLineWidth.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                val secondaryLabelLayout: TextLayoutResult? = if (secondaryLabelText == null) {
                    null
                } else {
                    textMeasurer.measure(
                        text = secondaryLabelText,
                        style = secondaryLabelStyle,
                        maxLines = SINGLE_LINE
                    )
                }
                val aboveOffset: Float = -labelCenterAbovePoint.toPx()
                val belowOffset: Float = labelCenterBelowPoint.toPx()
                val labelOffset: Float = if (isSecondaryAbove) {
                    belowOffset
                } else {
                    aboveOffset
                }
                val secondaryLabelOffset: Float = if (isSecondaryAbove) {
                    aboveOffset
                } else {
                    belowOffset
                }
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
                val labelCenterY: Float = slicePoints.y(pointIndex = index) + labelOffset
                val arrowColorFilter: ColorFilter = ColorFilter.tint(color = lineColor)
                onDrawBehind {
                    clipRect {
                        if (lowerArea == null) {
                            drawPath(
                                path = upperArea,
                                brush = lineBrush,
                                alpha = AREA_ALPHA
                            )
                        } else {
                            clipPath(path = upperArea) {
                                clipPath(
                                    path = lowerArea,
                                    clipOp = ClipOp.Difference
                                ) {
                                    drawRect(
                                        brush = lineBrush,
                                        alpha = AREA_ALPHA
                                    )
                                }
                            }
                        }
                        if (secondaryLine != null) {
                            drawPath(
                                path = secondaryLine,
                                brush = lineBrush,
                                alpha = HourChartSliceDefaults.SECONDARY_LINE_ALPHA,
                                style = secondaryStroke
                            )
                        }
                        drawPath(
                            path = line,
                            brush = lineBrush,
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
                    if (secondaryPoints != null && secondaryLabelLayout != null) {
                        drawText(
                            textLayoutResult = secondaryLabelLayout,
                            color = secondaryLabelColor,
                            topLeft = Offset(
                                x = (size.width - secondaryLabelLayout.size.width) / HALF_DIVISOR,
                                y = secondaryPoints.y(pointIndex = index) + secondaryLabelOffset - secondaryLabelLayout.size.height / HALF_DIVISOR
                            )
                        )
                    }
                }
            }
    )
}

private fun SlicePoints<*>.line(): Path {
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

private fun SlicePoints<*>.area(
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