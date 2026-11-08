package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.icons.VadretIcon
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretCard
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.degree
import se.vadretapp.vadret.ui.resources.feels_like
import se.vadretapp.vadret.ui.resources.humidity
import se.vadretapp.vadret.ui.resources.ic_north
import se.vadretapp.vadret.ui.resources.m_s
import se.vadretapp.vadret.ui.resources.percent
import se.vadretapp.vadret.ui.resources.wind
import se.vadretapp.vadret.weatherforecast.model.CurrentWeather

private val cardPadding = 21.dp
private val cardShape = RoundedCornerShape(size = 21.dp)
private val weatherIconSize = 64.dp
private val conditionSpacing = 8.dp
private val dividerTopPadding = 21.dp
private val dividerBottomPadding = 13.dp
private val statSpacing = 8.dp
private val statLabelSpacing = 3.dp
private val windArrowSize = 16.dp
private const val STAT_WEIGHT = 1f
private const val SINGLE_LINE = 1

@Composable
internal fun CurrentWeatherCard(
    currentWeather: CurrentWeather,
    modifier: Modifier = Modifier
) {
    val degree: String = stringResource(resource = Res.string.degree)
    VadretCard(
        modifier = modifier,
        shape = cardShape
    ) {
        Column(modifier = Modifier.padding(all = cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(space = conditionSpacing)
                ) {
                    AnimatedContent(
                        targetState = "${currentWeather.temperatureText}$degree",
                        transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) }
                    ) {
                        Text(
                            text = it,
                            style = VadretTheme.textStyles.temperature,
                            color = VadretTheme.colorScheme.onSurface
                        )
                    }
                    AnimatedContent(
                        targetState = stringResource(resource = currentWeather.descriptionResource),
                        transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) }
                    ) {
                        Text(
                            text = it,
                            style = VadretTheme.textStyles.condition,
                            color = VadretTheme.colorScheme.onSurface
                        )
                    }
                }
                VadretIcon(
                    icon = currentWeather.icon,
                    contentDescription = stringResource(resource = currentWeather.descriptionResource),
                    tint = VadretTheme.colors.icon,
                    modifier = Modifier.size(size = weatherIconSize)
                )
            }
            HorizontalDivider(
                color = VadretTheme.colors.divider,
                modifier = Modifier.padding(
                    top = dividerTopPadding,
                    bottom = dividerBottomPadding
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = statSpacing)
            ) {
                Stat(
                    label = stringResource(resource = Res.string.feels_like),
                    value = "${currentWeather.feelsLikeTemperatureText}$degree",
                    modifier = Modifier.weight(weight = STAT_WEIGHT)
                )
                Stat(
                    label = stringResource(resource = Res.string.wind),
                    value = "${currentWeather.windSpeed} ${stringResource(resource = Res.string.m_s)}",
                    modifier = Modifier.weight(weight = STAT_WEIGHT),
                    trailing = {
                        Icon(
                            painter = painterResource(resource = Res.drawable.ic_north),
                            contentDescription = stringResource(resource = currentWeather.windDirectionResource),
                            tint = VadretTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(size = windArrowSize)
                                .rotate(degrees = currentWeather.windArrowRotation)
                        )
                    }
                )
                Stat(
                    label = stringResource(resource = Res.string.humidity),
                    value = "${currentWeather.humidityPercent}${stringResource(resource = Res.string.percent)}",
                    modifier = Modifier.weight(weight = STAT_WEIGHT)
                )
            }
        }
    }
}

@Composable
private fun Stat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(space = statLabelSpacing),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = VadretTheme.textStyles.detailLabel,
            color = VadretTheme.colorScheme.onSurfaceVariant,
            maxLines = SINGLE_LINE,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = statLabelSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = VadretTheme.textStyles.reading,
                color = VadretTheme.colorScheme.onSurface,
                maxLines = SINGLE_LINE,
                softWrap = false
            )
            trailing()
        }
    }
}

@Preview
@Composable
private fun CurrentWeatherCardPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        CurrentWeatherCard(
            currentWeather = currentWeatherPreview(),
            modifier = Modifier
                .background(color = Color.DarkGray)
                .padding(all = VadretTheme.spacing.medium)
        )
    }
}