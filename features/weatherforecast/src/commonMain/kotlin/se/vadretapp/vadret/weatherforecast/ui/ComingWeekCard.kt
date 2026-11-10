package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.icons.VadretIcon
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretCard
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.degree
import se.vadretapp.vadret.ui.resources.today
import se.vadretapp.vadret.ui.resources.week_number
import se.vadretapp.vadret.weatherforecast.model.ComingWeek
import se.vadretapp.vadret.weatherforecast.model.WeekDay
import se.vadretapp.vadret.weatherforecast.ui.ComingWeekCard.OuterShape

private const val SINGLE_LINE = 1
private const val FIRST_INDEX = 0
private const val FULL_LEVEL = 1f
private const val HALF_DIVISOR = 2f
private const val SMOOTHING_DIVISOR = 6f
private const val BAND_ALPHA = 0.12f
private const val LOW_LINE_ALPHA = 0.45f
private const val DAY_WEIGHT = 1f

private val cardShape = RoundedCornerShape(size = 21.dp)
private val cardVerticalPadding = 13.dp
private val headerMinHeight = 34.dp
private val headerHorizontalPadding = 20.dp
private val headerVerticalPadding = 4.dp
private val headerSpacing = 13.dp
private val daysHorizontalPadding = 10.dp
private val todayShape = RoundedCornerShape(size = 12.dp)
private val dayTopPadding = 8.dp
private val dayContentSpacing = 5.dp
private val dayIconSize = 22.dp
private val chartHeight = 84.dp
private val lineTop = 16.dp
private val lineRange = 47.dp
private val highLabelCenterAbovePoint = 11.dp
private val lowLabelCenterBelowPoint = 12.dp
private val highLineWidth = 2.5.dp
private val lowLineWidth = 2.dp
private val dotRadius = 3.dp
private val dotRingRadius = 5.dp
private val weekdayAutoSize = TextAutoSize.StepBased(
    minFontSize = 9.sp,
    maxFontSize = 12.sp
)

private class WeekChartLines(
    val high: Path,
    val low: Path,
    val band: Path
)

object ComingWeekCard {
    val OuterPadding = 12.dp
    private val InnerRadius = 16.dp
    private val OuterRadius = OuterPadding + InnerRadius

    val OuterShape = RoundedCornerShape(OuterRadius)
    val InnerShape = RoundedCornerShape(InnerRadius)
}

@Composable
internal fun ComingWeekCard(
    comingWeek: ComingWeek,
    onDayClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer: TextMeasurer = rememberTextMeasurer()
    VadretCard(
        modifier = modifier,
        shape = OuterShape
    ) {
        Column(modifier = Modifier.padding(vertical = ComingWeekCard.OuterPadding)) {
            ComingWeekHeader(comingWeek = comingWeek)
            Row(modifier = Modifier.padding(horizontal = ComingWeekCard.OuterPadding)) {
                comingWeek.days.forEachIndexed { index: Int, weekDay: WeekDay ->
                    key(weekDay.dayKey) {
                        WeekDayColumn(
                            modifier = Modifier.weight(weight = DAY_WEIGHT),
                            shape = ComingWeekCard.InnerShape,
                            days = comingWeek.days,
                            index = index,
                            textMeasurer = textMeasurer,
                            onClick = { onDayClick(weekDay.dayKey) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComingWeekHeader(
    comingWeek: ComingWeek,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = headerMinHeight)
            .padding(
                start = headerHorizontalPadding,
                end = headerHorizontalPadding,
                bottom = 8.dp,
                top = 0.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(
            space = headerSpacing,
            alignment = Alignment.Start
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(weight = DAY_WEIGHT),
            text = stringResource(
                resource = Res.string.week_number,
                comingWeek.weekNumberText
            ),
            style = VadretTheme.textStyles.value,
            color = VadretTheme.colorScheme.onSurface,
            maxLines = SINGLE_LINE
        )
        Text(
            text = comingWeek.dateRangeText,
            style = VadretTheme.textStyles.detailLabel,
            color = VadretTheme.colorScheme.onSurfaceVariant,
            maxLines = SINGLE_LINE
        )
    }
}

@Composable
private fun WeekDayColumn(
    days: ImmutableList<WeekDay>,
    index: Int,
    textMeasurer: TextMeasurer,
    onClick: () -> Unit,
    shape: Shape ,
    modifier: Modifier = Modifier
) {
    val weekDay: WeekDay = days[index]
    val highlightColor: Color = if (weekDay.isToday) {
        VadretTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    Column(
        modifier = modifier
            .background(
                color = highlightColor,
                shape = shape
            )
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(top = dayTopPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = dayContentSpacing)
    ) {
        WeekdayLabel(weekDay = weekDay)
        VadretIcon(
            modifier = Modifier.size(size = dayIconSize),
            icon = weekDay.icon,
            contentDescription = stringResource(resource = weekDay.descriptionResource),
            tint = VadretTheme.colors.icon
        )
        Text(
            text = weekDay.precipitationText.orEmpty(),
            style = VadretTheme.textStyles.label,
            color = VadretTheme.colors.precipitation,
            maxLines = SINGLE_LINE
        )
        WeekTemperatureSlice(
            days = days,
            index = index,
            textMeasurer = textMeasurer,
            ringColor = highlightColor
        )
    }
}

@Composable
private fun WeekdayLabel(
    weekDay: WeekDay,
    modifier: Modifier = Modifier
) {
    if (weekDay.isToday) {
        Text(
            modifier = modifier,
            text = stringResource(resource = Res.string.today),
            style = VadretTheme.textStyles.labelStrong,
            color = VadretTheme.colorScheme.onSurface,
            autoSize = weekdayAutoSize,
            maxLines = SINGLE_LINE
        )
    } else {
        Text(
            modifier = modifier,
            text = weekDay.weekdayText,
            style = VadretTheme.textStyles.label,
            color = VadretTheme.colorScheme.onSurfaceVariant,
            autoSize = weekdayAutoSize,
            maxLines = SINGLE_LINE
        )
    }
}

// Each day draws its own slice of the week's curves, so the whole day column stays one tap target.
@Composable
private fun WeekTemperatureSlice(
    days: ImmutableList<WeekDay>,
    index: Int,
    textMeasurer: TextMeasurer,
    ringColor: Color,
    modifier: Modifier = Modifier
) {
    val weekDay: WeekDay = days[index]
    val degree: String = stringResource(resource = Res.string.degree)
    val highText = "${weekDay.highTemperatureText}$degree"
    val lowText = "${weekDay.lowTemperatureText}$degree"
    val lineColor: Color = VadretTheme.colors.temperatureWarm
    val highTextStyle: TextStyle = VadretTheme.textStyles.detailLabelStrong
    val highTextColor: Color = VadretTheme.colorScheme.onSurface
    val lowTextStyle: TextStyle = VadretTheme.textStyles.detailLabel
    val lowTextColor: Color = VadretTheme.colorScheme.onSurfaceVariant
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(height = chartHeight)
            .semantics { contentDescription = "$highText $lowText" }
            .drawWithCache {
                val columnWidth: Float = size.width
                val highPoints: List<Offset> = days.points(
                    shownIndex = index,
                    columnWidth = columnWidth,
                    lineTopPixels = lineTop.toPx(),
                    lineRangePixels = lineRange.toPx(),
                    level = { shownDay: WeekDay -> shownDay.highTemperatureLevel }
                )
                val lowPoints: List<Offset> = days.points(
                    shownIndex = index,
                    columnWidth = columnWidth,
                    lineTopPixels = lineTop.toPx(),
                    lineRangePixels = lineRange.toPx(),
                    level = { shownDay: WeekDay -> shownDay.lowTemperatureLevel }
                )
                val lines: WeekChartLines = weekChartLines(
                    highPoints = highPoints,
                    lowPoints = lowPoints
                )
                val highStroke = Stroke(
                    width = highLineWidth.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                val lowStroke = Stroke(
                    width = lowLineWidth.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                val highPoint: Offset = highPoints[index]
                val lowPoint: Offset = lowPoints[index]
                val highLayout: TextLayoutResult = textMeasurer.measure(
                    text = highText,
                    style = highTextStyle,
                    maxLines = SINGLE_LINE
                )
                val lowLayout: TextLayoutResult = textMeasurer.measure(
                    text = lowText,
                    style = lowTextStyle,
                    maxLines = SINGLE_LINE
                )
                onDrawBehind {
                    clipRect {
                        drawPath(
                            path = lines.band,
                            color = lineColor.copy(alpha = BAND_ALPHA)
                        )
                        drawPath(
                            path = lines.low,
                            color = lineColor.copy(alpha = LOW_LINE_ALPHA),
                            style = lowStroke
                        )
                        drawPath(
                            path = lines.high,
                            color = lineColor,
                            style = highStroke
                        )
                    }
                    if (weekDay.isToday) {
                        drawTodayDot(
                            center = highPoint,
                            color = lineColor,
                            ringColor = ringColor
                        )
                        drawTodayDot(
                            center = lowPoint,
                            color = lineColor,
                            ringColor = ringColor
                        )
                    }
                    drawText(
                        textLayoutResult = highLayout,
                        color = highTextColor,
                        topLeft = Offset(
                            x = (columnWidth - highLayout.size.width) / HALF_DIVISOR,
                            y = highPoint.y - highLabelCenterAbovePoint.toPx() - highLayout.size.height / HALF_DIVISOR
                        )
                    )
                    drawText(
                        textLayoutResult = lowLayout,
                        color = lowTextColor,
                        topLeft = Offset(
                            x = (columnWidth - lowLayout.size.width) / HALF_DIVISOR,
                            y = lowPoint.y + lowLabelCenterBelowPoint.toPx() - lowLayout.size.height / HALF_DIVISOR
                        )
                    )
                }
            }
    )
}

private fun ImmutableList<WeekDay>.points(
    shownIndex: Int,
    columnWidth: Float,
    lineTopPixels: Float,
    lineRangePixels: Float,
    level: (WeekDay) -> Float
): List<Offset> =
    mapIndexed { pointIndex: Int, weekDay: WeekDay ->
        Offset(
            x = (pointIndex - shownIndex) * columnWidth + columnWidth / HALF_DIVISOR,
            y = lineTopPixels + (FULL_LEVEL - level(weekDay)) * lineRangePixels
        )
    }

private fun DrawScope.drawTodayDot(
    center: Offset,
    color: Color,
    ringColor: Color
) {
    drawCircle(
        color = ringColor,
        radius = dotRingRadius.toPx(),
        center = center
    )
    drawCircle(
        color = color,
        radius = dotRadius.toPx(),
        center = center
    )
}

private fun weekChartLines(
    highPoints: List<Offset>,
    lowPoints: List<Offset>
): WeekChartLines {
    val high = Path()
    high.moveTo(
        x = highPoints.first().x,
        y = highPoints.first().y
    )
    high.curveThrough(points = highPoints)
    val low = Path()
    low.moveTo(
        x = lowPoints.first().x,
        y = lowPoints.first().y
    )
    low.curveThrough(points = lowPoints)
    val band = Path()
    band.addPath(path = high)
    band.lineTo(
        x = lowPoints.last().x,
        y = lowPoints.last().y
    )
    band.curveThrough(points = lowPoints.asReversed())
    band.close()
    return WeekChartLines(
        high = high,
        low = low,
        band = band
    )
}

private fun Path.curveThrough(points: List<Offset>) {
    for (pointIndex in FIRST_INDEX until points.lastIndex) {
        val previous: Offset = points[(pointIndex - 1).coerceAtLeast(minimumValue = FIRST_INDEX)]
        val start: Offset = points[pointIndex]
        val end: Offset = points[pointIndex + 1]
        val next: Offset = points[(pointIndex + 2).coerceAtMost(maximumValue = points.lastIndex)]
        cubicTo(
            x1 = start.x + (end.x - previous.x) / SMOOTHING_DIVISOR,
            y1 = start.y + (end.y - previous.y) / SMOOTHING_DIVISOR,
            x2 = end.x - (next.x - start.x) / SMOOTHING_DIVISOR,
            y2 = end.y - (next.y - start.y) / SMOOTHING_DIVISOR,
            x3 = end.x,
            y3 = end.y
        )
    }
}

@Preview
@Composable
private fun ComingWeekCardPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        ComingWeekCard(
            modifier = Modifier
                .background(color = VadretTheme.colorScheme.background)
                .padding(all = VadretTheme.spacing.medium),
            comingWeek = comingWeekPreview(),
            onDayClick = {}
        )
    }
}