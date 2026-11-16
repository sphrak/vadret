package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.HourChartSliceDefaults
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretColors
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.average_wind
import se.vadretapp.vadret.ui.resources.feels_like
import se.vadretapp.vadret.ui.resources.hour_chart_legend
import se.vadretapp.vadret.ui.resources.ic_info
import se.vadretapp.vadret.ui.resources.temperature
import se.vadretapp.vadret.ui.resources.wind_gusts
import se.vadretapp.vadret.weatherforecast.model.TemperatureShadeStop

private const val HALF_DIVISOR = 2f
private const val LINE_START = 0f
private const val FULL_ALPHA = 1f

private val tooltipBorderWidth = 1.dp
private val legendRowSpacing = 4.dp
private val legendLabelSpacing = 8.dp
private val legendLineLength = 32.dp
private val legendLineHeight = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HourChartLegendButton(
    temperatureShadeStops: ImmutableList<TemperatureShadeStop>,
    feelsLikeShadeStops: ImmutableList<TemperatureShadeStop>,
    windColor: Color,
    modifier: Modifier = Modifier
) {
    val tooltipState: TooltipState = rememberTooltipState(isPersistent = true)
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    Box(modifier = modifier) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning = TooltipAnchorPosition.Above),
            tooltip = {
                RichTooltip(
                    modifier = Modifier
                        .padding(horizontal = VadretTheme.spacing.large)
                        .border(
                            width = tooltipBorderWidth,
                            color = VadretTheme.colorScheme.outlineVariant,
                            shape = VadretTheme.shapes.large
                        ),
                    shape = VadretTheme.shapes.large,
                    colors = TooltipDefaults.richTooltipColors(containerColor = VadretTheme.colorScheme.surface),
                    text = {
                        HourChartLegend(
                            modifier = Modifier.padding(vertical = VadretTheme.spacing.small),
                            temperatureShadeStops = temperatureShadeStops,
                            feelsLikeShadeStops = feelsLikeShadeStops,
                            windColor = windColor
                        )
                    }
                )
            },
            state = tooltipState
        ) {
            IconButton(
                modifier = Modifier.size(size = IconButtonDefaults.extraSmallContainerSize()),
                onClick = { coroutineScope.launch { tooltipState.show() } }
            ) {
                Icon(
                    modifier = Modifier.size(size = IconButtonDefaults.extraSmallIconSize),
                    painter = painterResource(resource = Res.drawable.ic_info),
                    contentDescription = stringResource(resource = Res.string.hour_chart_legend),
                    tint = VadretTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HourChartLegend(
    temperatureShadeStops: ImmutableList<TemperatureShadeStop>,
    feelsLikeShadeStops: ImmutableList<TemperatureShadeStop>,
    windColor: Color,
    modifier: Modifier = Modifier
) {
    val windBrush: Brush = SolidColor(value = windColor)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = legendRowSpacing)
    ) {
        LegendRow(
            label = stringResource(resource = Res.string.temperature),
            lineBrush = legendBrush(shadeStops = temperatureShadeStops),
            lineAlpha = FULL_ALPHA,
            lineWidth = HourChartSliceDefaults.LineWidth
        )
        LegendRow(
            label = stringResource(resource = Res.string.feels_like),
            lineBrush = legendBrush(shadeStops = feelsLikeShadeStops),
            lineAlpha = HourChartSliceDefaults.SECONDARY_LINE_ALPHA,
            lineWidth = HourChartSliceDefaults.SecondaryLineWidth
        )
        LegendRow(
            label = stringResource(resource = Res.string.average_wind),
            lineBrush = windBrush,
            lineAlpha = FULL_ALPHA,
            lineWidth = HourChartSliceDefaults.LineWidth
        )
        LegendRow(
            label = stringResource(resource = Res.string.wind_gusts),
            lineBrush = windBrush,
            lineAlpha = HourChartSliceDefaults.SECONDARY_LINE_ALPHA,
            lineWidth = HourChartSliceDefaults.SecondaryLineWidth
        )
    }
}

@Composable
private fun legendBrush(shadeStops: ImmutableList<TemperatureShadeStop>): Brush {
    val colors: VadretColors = VadretTheme.colors
    val density: Density = LocalDensity.current
    return remember(shadeStops, colors, density) {
        with(receiver = density) {
            shadeStops.legendShading(
                lineLengthPixels = legendLineLength.toPx(),
                colors = colors
            )
        }
    }
}

@Composable
private fun LegendRow(
    label: String,
    lineBrush: Brush,
    lineAlpha: Float,
    lineWidth: Dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = legendLabelSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .size(
                    width = legendLineLength,
                    height = legendLineHeight
                )
                .drawBehind {
                    drawLine(
                        brush = lineBrush,
                        start = Offset(
                            x = LINE_START,
                            y = size.height / HALF_DIVISOR
                        ),
                        end = Offset(
                            x = size.width,
                            y = size.height / HALF_DIVISOR
                        ),
                        strokeWidth = lineWidth.toPx(),
                        cap = StrokeCap.Round,
                        alpha = lineAlpha
                    )
                }
        )
        Text(
            text = label,
            style = VadretTheme.textStyles.detailLabel,
            color = VadretTheme.colorScheme.onSurface
        )
    }
}

@Preview
@Composable
private fun HourChartLegendPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        HourChartLegend(
            modifier = Modifier
                .background(color = VadretTheme.colorScheme.surface)
                .padding(all = VadretTheme.spacing.medium),
            temperatureShadeStops = comingDaysPreview().first().temperatureLegendShadeStops,
            feelsLikeShadeStops = comingDaysPreview().first().feelsLikeLegendShadeStops,
            windColor = VadretTheme.colorScheme.onSurfaceVariant
        )
    }
}