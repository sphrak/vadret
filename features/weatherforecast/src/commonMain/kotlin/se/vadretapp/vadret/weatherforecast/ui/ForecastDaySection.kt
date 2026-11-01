package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.degree
import se.vadretapp.vadret.core.resources.feels_like
import se.vadretapp.vadret.icons.VadretIcon
import se.vadretapp.vadret.ui.VadretCard
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.weatherforecast.model.ForecastDay
import se.vadretapp.vadret.weatherforecast.model.ForecastHour

private val dayHeaderPadding = 8.dp
private val cardShape = RoundedCornerShape(size = 21.dp)
private val cardVerticalPadding = 10.5.dp
private val hourRowMinHeight = 55.dp
private val hourRowHorizontalPadding = 21.dp
private val hourRowSpacing = 8.dp
private val hourTimeWidth = 44.dp
private val hourIconSize = 34.dp
private val temperatureSpacing = 2.dp
private const val DESCRIPTION_MAX_LINES = 2
private const val SINGLE_LINE = 1
private const val DESCRIPTION_WEIGHT = 1f

@Composable
internal fun ForecastDayHeader(
    forecastDay: ForecastDay,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dayHeaderPadding,
                end = dayHeaderPadding,
                top = dayHeaderPadding
            )
    ) {
        Text(
            text = forecastDay.weekday,
            style = VadretTheme.textStyles.title,
            color = VadretTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(weight = 1f)
                .alignByBaseline()
        )
        Text(
            text = forecastDay.dateText,
            style = VadretTheme.textStyles.detailLabel,
            color = VadretTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alignByBaseline()
        )
    }
}

@Composable
internal fun ForecastDayCard(
    forecastDay: ForecastDay,
    modifier: Modifier = Modifier
) {
    VadretCard(
        modifier = modifier,
        shape = cardShape
    ) {
        Column(modifier = Modifier.padding(vertical = cardVerticalPadding)) {
            forecastDay.hours.forEachIndexed { index: Int, forecastHour: ForecastHour ->
                if (index > 0) {
                    HorizontalDivider(
                        color = VadretTheme.colors.divider,
                        modifier = Modifier.padding(horizontal = hourRowHorizontalPadding)
                    )
                }
                ForecastHourRow(forecastHour = forecastHour)
            }
        }
    }
}

@Composable
private fun ForecastHourRow(
    forecastHour: ForecastHour,
    modifier: Modifier = Modifier
) {
    val degree: String = stringResource(resource = Res.string.degree)
    val feelsLikeTemperature: String? = forecastHour.feelsLikeTemperature
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = hourRowSpacing),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = hourRowMinHeight)
            .padding(horizontal = hourRowHorizontalPadding)
    ) {
        Text(
            text = forecastHour.timeText,
            style = VadretTheme.textStyles.value,
            color = VadretTheme.colorScheme.onSurface,
            modifier = Modifier.width(width = hourTimeWidth)
        )
        VadretIcon(
            icon = forecastHour.icon,
            contentDescription = null,
            tint = VadretTheme.colors.icon,
            modifier = Modifier.size(size = hourIconSize)
        )
        Text(
            text = stringResource(resource = forecastHour.descriptionResource),
            style = VadretTheme.textStyles.listItem,
            color = VadretTheme.colorScheme.onSurface,
            maxLines = DESCRIPTION_MAX_LINES,
            modifier = Modifier.weight(weight = DESCRIPTION_WEIGHT)
        )
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(space = temperatureSpacing)
        ) {
            Text(
                text = "${forecastHour.temperature}$degree",
                style = VadretTheme.textStyles.value,
                color = VadretTheme.colorScheme.onSurface
            )
            if (feelsLikeTemperature != null) {
                Text(
                    text = "${stringResource(resource = Res.string.feels_like)} $feelsLikeTemperature$degree",
                    style = VadretTheme.textStyles.detailLabel,
                    color = VadretTheme.colorScheme.onSurfaceVariant,
                    maxLines = SINGLE_LINE,
                    softWrap = false
                )
            }
        }
    }
}