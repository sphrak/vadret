package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.icons.VadretIcon
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.HourChartSlice
import se.vadretapp.vadret.ui.HourColumn
import se.vadretapp.vadret.ui.HourLabel
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretColors
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.model.HourChartLinePlacement
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.degree
import se.vadretapp.vadret.ui.resources.feels_like
import se.vadretapp.vadret.ui.resources.ic_north
import se.vadretapp.vadret.ui.resources.m_s
import se.vadretapp.vadret.ui.resources.wind_gusts
import se.vadretapp.vadret.weatherforecast.model.ComingDay
import se.vadretapp.vadret.weatherforecast.model.ComingHour
import se.vadretapp.vadret.weatherforecast.model.TemperatureShadeStop

private const val SINGLE_LINE = 1
private const val SHOWN_HOUR_COUNT = 7
private const val ALL_SHOWN = 1f
private const val NOT_SCROLLED = 0f
private const val FULLY_SCROLLED = 1f
private const val NO_HOURS = 0
private const val FIRST_HOUR_INDEX = 0
private const val HALF_DIVISOR = 2f
private const val FULL_LEVEL = 1f
private val hourCellContentSpacing = 5.dp
private val hourCellBottomPadding = 4.dp
private val hourIconSize = 22.dp
private val temperatureChartSpacing = 6.dp
private val windChartSpacing = 8.dp
private val chartHeight = 89.dp
private val chartLineTop = 20.dp
private val chartLineRange = 49.dp
private val dotRadius = 3.dp
private val dotRingRadius = 5.dp
private val legendIconInset = 6.dp
private val legendButtonPadding = ComingDayCard.Inset - legendIconInset

private object HourPageSize : PageSize {

    override fun Density.calculateMainAxisPageSize(
        availableSpace: Int,
        pageSpacing: Int
    ): Int = availableSpace / SHOWN_HOUR_COUNT
}

@Composable
internal fun ComingHourStrip(
    comingDay: ComingDay,
    modifier: Modifier = Modifier
) {
    val hours: ImmutableList<ComingHour> = comingDay.hours
    val textMeasurer: TextMeasurer = rememberTextMeasurer()
    val temperatureColor: Color = VadretTheme.colors.temperatureWarm
    val temperatureBrush: Brush = temperatureBrush(shadeStops = comingDay.hourShadeStops)
    val windColor: Color = VadretTheme.colorScheme.onSurfaceVariant
    val pagerState: PagerState = rememberPagerState(
        initialPage = comingDay.firstShownHourIndex,
        pageCount = { hours.size }
    )
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ComingDayCard.Inset),
            state = pagerState,
            contentPadding = PaddingValues(top = ComingDayCard.Inset),
            pageSize = HourPageSize,
            verticalAlignment = Alignment.Top,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                pagerSnapDistance = PagerSnapDistance.atMost(pages = SHOWN_HOUR_COUNT)
            ),
            key = { index: Int -> hours[index].key }
        ) { index: Int ->
            ComingHourColumn(
                modifier = Modifier.fillMaxWidth(),
                hours = hours,
                index = index,
                textMeasurer = textMeasurer,
                temperatureColor = temperatureColor,
                temperatureBrush = temperatureBrush,
                windColor = windColor
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = legendButtonPadding)
        ) {
            HourScrollIndicator(
                modifier = Modifier.align(alignment = Alignment.Center),
                shownFraction = hours.shownFraction(),
                scrolledFraction = { pagerState.scrolledFraction() }
            )
            HourChartLegendButton(
                modifier = Modifier
                    .align(alignment = Alignment.CenterEnd)
                    .padding(end = legendButtonPadding),
                temperatureShadeStops = comingDay.temperatureLegendShadeStops,
                feelsLikeShadeStops = comingDay.feelsLikeLegendShadeStops,
                windColor = windColor
            )
        }
    }
}

@Composable
private fun temperatureBrush(shadeStops: ImmutableList<TemperatureShadeStop>): Brush {
    val colors: VadretColors = VadretTheme.colors
    val density: Density = LocalDensity.current
    return remember(shadeStops, colors, density) {
        with(receiver = density) {
            shadeStops.shading(
                lineTopPixels = chartLineTop.toPx(),
                lineRangePixels = chartLineRange.toPx(),
                colors = colors
            )
        }
    }
}

private fun ImmutableList<ComingHour>.shownFraction(): Float =
    if (size <= SHOWN_HOUR_COUNT) {
        ALL_SHOWN
    } else {
        SHOWN_HOUR_COUNT.toFloat() / size
    }

private fun PagerState.scrolledFraction(): Float {
    val hiddenHourCount: Int = pageCount - SHOWN_HOUR_COUNT
    if (hiddenHourCount <= NO_HOURS) {
        return NOT_SCROLLED
    }
    return ((currentPage + currentPageOffsetFraction) / hiddenHourCount).coerceIn(
        minimumValue = NOT_SCROLLED,
        maximumValue = FULLY_SCROLLED
    )
}

@Composable
private fun ComingHourColumn(
    hours: ImmutableList<ComingHour>,
    index: Int,
    textMeasurer: TextMeasurer,
    temperatureColor: Color,
    temperatureBrush: Brush,
    windColor: Color,
    modifier: Modifier = Modifier
) {
    val comingHour: ComingHour = hours[index]
    val degree: String = stringResource(resource = Res.string.degree)
    val temperatureText = "${comingHour.temperatureText}$degree"
    val feelsLikeText = "${comingHour.feelsLikeTemperatureText}$degree"
    val temperatureDescription = "$temperatureText, ${stringResource(resource = Res.string.feels_like)} $feelsLikeText"
    val meterPerSecond: String = stringResource(resource = Res.string.m_s)
    val windText = "${stringResource(resource = comingHour.windDirectionResource)} ${comingHour.windSpeedText} $meterPerSecond"
    val windDescription: String = if (comingHour.windGustSpeedText == null) {
        windText
    } else {
        "$windText, ${stringResource(resource = Res.string.wind_gusts)} ${comingHour.windGustSpeedText} $meterPerSecond"
    }
    val dotRingColor: Color = VadretTheme.colorScheme.surface
    HourColumn(
        modifier = modifier,
        isNow = comingHour.isNow,
        shape = ComingDayCard.InnerShape
    ) {
        ComingHourCell(comingHour = comingHour)
        HourChartSlice(
            hours = hours,
            index = index,
            level = { shownHour: ComingHour -> shownHour.temperatureLevel },
            lineColor = temperatureColor,
            lineTop = chartLineTop,
            lineRange = chartLineRange,
            labelText = temperatureText,
            labelStyle = VadretTheme.textStyles.detailLabelStrong,
            labelColor = VadretTheme.colorScheme.onSurface,
            textMeasurer = textMeasurer,
            lineBrush = temperatureBrush,
            secondaryLevel = { shownHour: ComingHour -> shownHour.feelsLikeLevel },
            secondaryLabelText = feelsLikeText,
            secondaryLabelStyle = VadretTheme.textStyles.label,
            secondaryLabelColor = VadretTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = temperatureChartSpacing)
                .height(height = chartHeight)
                .semantics { contentDescription = temperatureDescription }
                .then(
                    other = if (index == FIRST_HOUR_INDEX) {
                        Modifier.startDot(
                            level = comingHour.temperatureLevel,
                            lineTop = chartLineTop,
                            lineRange = chartLineRange,
                            brush = temperatureBrush,
                            ringColor = dotRingColor
                        )
                    } else {
                        Modifier
                    }
                )
        )
        HourChartSlice(
            hours = hours,
            index = index,
            level = { shownHour: ComingHour -> shownHour.windLevel },
            lineColor = windColor,
            lineTop = chartLineTop,
            lineRange = chartLineRange,
            labelText = comingHour.windSpeedText,
            labelStyle = VadretTheme.textStyles.detailLabelStrong,
            labelColor = VadretTheme.colorScheme.onSurface,
            textMeasurer = textMeasurer,
            secondaryLevel = { shownHour: ComingHour -> shownHour.windGustLevel },
            secondaryPlacement = HourChartLinePlacement.ABOVE,
            secondaryLabelText = comingHour.windGustSpeedText,
            secondaryLabelStyle = VadretTheme.textStyles.label,
            secondaryLabelColor = VadretTheme.colorScheme.onSurfaceVariant,
            arrow = painterResource(resource = Res.drawable.ic_north),
            arrowRotation = comingHour.windArrowRotation,
            modifier = Modifier
                .padding(top = windChartSpacing)
                .height(height = chartHeight)
                .semantics { contentDescription = windDescription }
                .then(
                    other = if (index == FIRST_HOUR_INDEX) {
                        Modifier.startDot(
                            level = comingHour.windLevel,
                            lineTop = chartLineTop,
                            lineRange = chartLineRange,
                            brush = SolidColor(value = windColor),
                            ringColor = dotRingColor
                        )
                    } else {
                        Modifier
                    }
                )
        )
    }
}

private fun Modifier.startDot(
    level: Float,
    lineTop: Dp,
    lineRange: Dp,
    brush: Brush,
    ringColor: Color
): Modifier =
    drawWithContent {
        drawContent()
        val center = Offset(
            x = size.width / HALF_DIVISOR,
            y = lineTop.toPx() + (FULL_LEVEL - level) * lineRange.toPx()
        )
        drawCircle(
            color = ringColor,
            radius = dotRingRadius.toPx(),
            center = center
        )
        drawCircle(
            brush = brush,
            radius = dotRadius.toPx(),
            center = center
        )
    }

@Composable
private fun ComingHourCell(
    comingHour: ComingHour,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = hourCellContentSpacing),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = hourCellBottomPadding)
    ) {
        HourLabel(
            timeText = comingHour.timeText,
            isNow = comingHour.isNow
        )
        VadretIcon(
            icon = comingHour.icon,
            contentDescription = stringResource(resource = comingHour.descriptionResource),
            tint = VadretTheme.colors.icon,
            modifier = Modifier.size(size = hourIconSize)
        )
        Text(
            text = comingHour.precipitationText.orEmpty(),
            style = VadretTheme.textStyles.label,
            color = VadretTheme.colors.precipitation,
            maxLines = SINGLE_LINE
        )
    }
}

@Preview
@Composable
private fun ComingHourColumnPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        ComingHourColumn(
            hours = comingHoursPreview(),
            index = nowHourIndexPreview(),
            textMeasurer = rememberTextMeasurer(),
            temperatureColor = VadretTheme.colors.temperatureWarm,
            temperatureBrush = temperatureBrush(shadeStops = comingDaysPreview().first().hourShadeStops),
            windColor = VadretTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .background(color = VadretTheme.colorScheme.surface)
                .padding(all = VadretTheme.spacing.medium)
        )
    }
}

@Preview
@Composable
private fun ComingHourStripStartPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        ComingHourStrip(
            modifier = Modifier.background(color = VadretTheme.colorScheme.surface),
            comingDay = comingDaysPreview().first()
        )
    }
}