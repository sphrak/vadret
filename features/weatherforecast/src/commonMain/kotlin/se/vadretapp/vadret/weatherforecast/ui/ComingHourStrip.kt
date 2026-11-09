package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
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
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.hourColumnBottomPadding
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.degree
import se.vadretapp.vadret.ui.resources.ic_north
import se.vadretapp.vadret.ui.resources.m_s
import se.vadretapp.vadret.ui.resources.temperature
import se.vadretapp.vadret.ui.resources.wind_in_meters_per_second
import se.vadretapp.vadret.weatherforecast.model.ComingHour

private const val HOUR_CONTENT_TYPE = "coming-hour"
private const val SINGLE_LINE = 1
private val hourCellContentSpacing = 5.dp
private val hourCellVerticalPadding = 4.dp
private val hourIconSize = 22.dp
private val temperatureLegendSpacing = 6.dp
private val temperatureChartHeight = 64.dp
private val temperatureLineTop = 20.dp
private val temperatureLineRange = 36.dp
private val windLegendSpacing = 8.dp
private val windChartHeight = 52.dp
private val windLineTop = 22.dp
private val windLineRange = 26.dp
private val legendHeight = 18.dp
private val legendHorizontalPadding = 4.dp
private val legendSpacing = 6.dp
private val legendSwatchWidth = 12.dp
private val legendSwatchHeight = 3.dp
private val legendSwatchShape = RoundedCornerShape(size = 2.dp)

@Composable
internal fun ComingHourStrip(
    hours: ImmutableList<ComingHour>,
    firstShownHourIndex: Int,
    modifier: Modifier = Modifier
) {
    val textMeasurer: TextMeasurer = rememberTextMeasurer()
    val temperatureColor: Color = VadretTheme.colors.temperatureWarm
    val windColor: Color = VadretTheme.colorScheme.primary
    Box(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            state = rememberLazyListState(initialFirstVisibleItemIndex = firstShownHourIndex),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(
                items = hours,
                key = { _: Int, comingHour: ComingHour -> comingHour.key },
                contentType = { _: Int, _: ComingHour -> HOUR_CONTENT_TYPE }
            ) { index: Int, _: ComingHour ->
                ComingHourColumn(
                    hours = hours,
                    index = index,
                    textMeasurer = textMeasurer,
                    temperatureColor = temperatureColor,
                    windColor = windColor
                )
            }
        }
        ChartLegends(
            temperatureColor = temperatureColor,
            windColor = windColor,
            modifier = Modifier
                .align(alignment = Alignment.BottomStart)
                .padding(start = 16.dp)
        )
    }
}

@Composable
private fun ComingHourColumn(
    hours: ImmutableList<ComingHour>,
    index: Int,
    textMeasurer: TextMeasurer,
    temperatureColor: Color,
    windColor: Color,
    modifier: Modifier = Modifier
) {
    val comingHour: ComingHour = hours[index]
    val temperatureText = "${comingHour.temperatureText}${stringResource(resource = Res.string.degree)}"
    val windDescription = "${stringResource(resource = comingHour.windDirectionResource)} ${comingHour.windSpeedText} ${stringResource(resource = Res.string.m_s)}"
    HourColumn(
        isNow = comingHour.isNow,
        modifier = modifier
    ) {
        ComingHourCell(comingHour = comingHour)
        HourChartSlice(
            hours = hours,
            index = index,
            level = { shownHour: ComingHour -> shownHour.temperatureLevel },
            lineColor = temperatureColor,
            lineTop = temperatureLineTop,
            lineRange = temperatureLineRange,
            labelText = temperatureText,
            labelStyle = VadretTheme.textStyles.detailLabelStrong,
            labelColor = VadretTheme.colorScheme.onSurface,
            textMeasurer = textMeasurer,
            modifier = Modifier
                .padding(top = temperatureLegendSpacing + legendHeight)
                .height(height = temperatureChartHeight)
                .semantics { contentDescription = temperatureText }
        )
        HourChartSlice(
            hours = hours,
            index = index,
            level = { shownHour: ComingHour -> shownHour.windLevel },
            lineColor = windColor,
            lineTop = windLineTop,
            lineRange = windLineRange,
            labelText = comingHour.windSpeedText,
            labelStyle = VadretTheme.textStyles.label,
            labelColor = VadretTheme.colorScheme.onSurfaceVariant,
            textMeasurer = textMeasurer,
            arrow = painterResource(resource = Res.drawable.ic_north),
            arrowRotation = comingHour.windArrowRotation,
            modifier = Modifier
                .padding(top = windLegendSpacing + legendHeight)
                .height(height = windChartHeight)
                .semantics { contentDescription = windDescription }
        )
    }
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
            .padding(vertical = hourCellVerticalPadding)
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

@Composable
private fun ChartLegends(
    temperatureColor: Color,
    windColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ChartLegend(
            title = stringResource(resource = Res.string.temperature),
            color = temperatureColor
        )
        Spacer(modifier = Modifier.height(height = temperatureChartHeight + windLegendSpacing))
        ChartLegend(
            title = stringResource(resource = Res.string.wind_in_meters_per_second),
            color = windColor
        )
        Spacer(modifier = Modifier.height(height = windChartHeight + hourColumnBottomPadding))
    }
}

@Composable
private fun ChartLegend(
    title: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(height = legendHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = legendSpacing)
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = legendSwatchWidth,
                    height = legendSwatchHeight
                )
                .background(
                    color = color,
                    shape = legendSwatchShape
                )
        )
        Text(
            text = title,
            style = VadretTheme.textStyles.label,
            color = VadretTheme.colorScheme.onSurfaceVariant,
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
            windColor = VadretTheme.colorScheme.primary,
            modifier = Modifier
                .background(color = VadretTheme.colorScheme.surface)
                .padding(all = VadretTheme.spacing.medium)
        )
    }
}