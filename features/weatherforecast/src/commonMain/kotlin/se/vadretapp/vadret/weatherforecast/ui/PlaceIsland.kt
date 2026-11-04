package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.icons.VadretIcon
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.ui.VadretProgressBar
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.format.toOneDecimalText
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.close
import se.vadretapp.vadret.ui.resources.degree
import se.vadretapp.vadret.ui.resources.feels_like
import se.vadretapp.vadret.ui.resources.humidity
import se.vadretapp.vadret.ui.resources.ic_close
import se.vadretapp.vadret.ui.resources.ic_north
import se.vadretapp.vadret.ui.resources.m_s
import se.vadretapp.vadret.ui.resources.open_forecast
import se.vadretapp.vadret.ui.resources.percent
import se.vadretapp.vadret.ui.resources.save_place
import se.vadretapp.vadret.ui.resources.saved
import se.vadretapp.vadret.ui.resources.try_again
import se.vadretapp.vadret.ui.resources.unknown_area
import se.vadretapp.vadret.ui.resources.wind
import se.vadretapp.vadret.weatherforecast.PlaceIslandEvent
import se.vadretapp.vadret.weatherforecast.PlaceIslandState
import se.vadretapp.vadret.weatherforecast.PlaceIslandViewModel
import se.vadretapp.vadret.weatherforecast.PlaceOpening
import se.vadretapp.vadret.weatherforecast.model.CurrentWeather
import se.vadretapp.vadret.weatherforecast.model.PlaceNaming

private val islandShape = RoundedCornerShape(size = 21.dp)
private val islandPadding = 21.dp
private val islandElevation = 6.dp
private val weatherIconSize = 64.dp
private val conditionSpacing = 8.dp
private val sectionSpacing = 13.dp
private val dividerTopPadding = 21.dp
private val dividerBottomPadding = 13.dp
private val actionsTopPadding = 21.dp
private val statSpacing = 8.dp
private val statLabelSpacing = 3.dp
private val windArrowSize = 16.dp

// Lines the close icon up with the content edge instead of the edge of its larger touch area.
private val closeButtonInset = 12.dp
private const val STAT_WEIGHT = 1f
private const val ACTION_WEIGHT = 1f
private const val SINGLE_LINE = 1

@Composable
fun PlaceIslandRoute(
    latitude: Double,
    longitude: Double,
    onDismiss: () -> Unit,
    onOpenForecast: () -> Unit,
    onOpenChosenLocation: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaceIslandViewModel = koinViewModel()
) {
    val state: PlaceIslandState by viewModel.state.collectAsStateWithLifecycle()
    val opening: PlaceOpening? = state.opening
    if (opening != null) {
        LaunchedEffect(opening) {
            when (opening) {
                PlaceOpening.PLACE_FORECAST -> onOpenForecast()
                PlaceOpening.CHOSEN_LOCATION -> onOpenChosenLocation()
            }
            viewModel.onEvent(event = PlaceIslandEvent.OnOpeningShown)
        }
    }
    LaunchedEffect(latitude, longitude) {
        viewModel.onEvent(
            event = PlaceIslandEvent.OnPlaceDropped(
                latitude = latitude,
                longitude = longitude
            )
        )
    }
    val droppedLocation = Location(
        latitude = latitude,
        longitude = longitude
    )
    PlaceIsland(
        state = if (state.location == droppedLocation) {
            state
        } else {
            PlaceIslandState(
                location = droppedLocation,
                isLoading = true
            )
        },
        onEvent = viewModel::onEvent,
        onDismiss = onDismiss,
        modifier = modifier
    )
}

@Composable
internal fun PlaceIsland(
    state: PlaceIslandState,
    onEvent: (PlaceIslandEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val placeNaming: PlaceNaming? = state.placeNaming
    if (placeNaming != null) {
        PlaceLabelDialog(
            placeNaming = placeNaming,
            onLabelChanged = { label: String -> onEvent(PlaceIslandEvent.OnLabelChanged(label = label)) },
            onSave = { onEvent(PlaceIslandEvent.OnSaveClicked) },
            onDismiss = { onEvent(PlaceIslandEvent.OnLabelDismissed) }
        )
    }
    Surface(
        shape = islandShape,
        color = VadretTheme.colorScheme.surface,
        shadowElevation = islandElevation,
        modifier = modifier.fillMaxWidth()
    ) {
        PlaceIslandContent(
            state = state,
            onEvent = onEvent,
            onDismiss = onDismiss,
            modifier = Modifier.padding(all = islandPadding)
        )
    }
}

@Composable
private fun PlaceIslandContent(
    state: PlaceIslandState,
    onEvent: (PlaceIslandEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PlaceHeading(
            state = state,
            onDismiss = onDismiss
        )
        val currentWeather: CurrentWeather? = state.currentWeather
        val errorMessageResource: StringResource? = state.errorMessageResource
        if (currentWeather != null) {
            PlaceWeather(
                currentWeather = currentWeather,
                modifier = Modifier.padding(top = sectionSpacing)
            )
            PlaceActions(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.padding(top = actionsTopPadding)
            )
        } else if (state.isLoading) {
            VadretProgressBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = sectionSpacing)
            )
        } else {
            TextButton(onClick = { onEvent(PlaceIslandEvent.OnRetryClicked) }) {
                Text(text = stringResource(resource = Res.string.try_again))
            }
        }
        if (errorMessageResource != null) {
            Text(
                text = stringResource(resource = errorMessageResource),
                style = VadretTheme.textStyles.detail,
                color = VadretTheme.colorScheme.error,
                modifier = Modifier.padding(top = sectionSpacing)
            )
        }
    }
}

@Composable
private fun PlaceHeading(
    state: PlaceIslandState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(weight = 1f)) {
            Text(
                text = state.shownPlaceName ?: stringResource(resource = Res.string.unknown_area),
                style = VadretTheme.textStyles.title,
                color = VadretTheme.colorScheme.onSurface
            )
            val county: String? = state.county
            if (county != null) {
                Text(
                    text = county,
                    style = VadretTheme.textStyles.detailLabel,
                    color = VadretTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.offset(
                x = closeButtonInset,
                y = -closeButtonInset
            )
        ) {
            Icon(
                painter = painterResource(resource = Res.drawable.ic_close),
                contentDescription = stringResource(resource = Res.string.close),
                tint = VadretTheme.colors.icon
            )
        }
    }
}

@Composable
private fun PlaceWeather(
    currentWeather: CurrentWeather,
    modifier: Modifier = Modifier
) {
    val degree: String = stringResource(resource = Res.string.degree)
    val description: String = stringResource(resource = currentWeather.descriptionResource)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(space = conditionSpacing)) {
                Text(
                    text = "${currentWeather.temperatureText}$degree",
                    style = VadretTheme.textStyles.temperature,
                    color = VadretTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = VadretTheme.textStyles.condition,
                    color = VadretTheme.colorScheme.onSurface
                )
            }
            VadretIcon(
                icon = currentWeather.icon,
                contentDescription = description,
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
        PlaceStats(
            currentWeather = currentWeather,
            degree = degree
        )
    }
}

@Composable
private fun PlaceStats(
    currentWeather: CurrentWeather,
    degree: String,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = statSpacing),
        modifier = modifier.fillMaxWidth()
    ) {
        Stat(
            label = stringResource(resource = Res.string.feels_like),
            value = "${currentWeather.feelsLikeTemperatureText}$degree",
            modifier = Modifier.weight(weight = STAT_WEIGHT)
        )
        Stat(
            label = stringResource(resource = Res.string.wind),
            value = "${currentWeather.windSpeed.toOneDecimalText()} ${stringResource(resource = Res.string.m_s)}",
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

@Composable
private fun PlaceActions(
    state: PlaceIslandState,
    onEvent: (PlaceIslandEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        if (state.isSaved) {
            Text(
                text = stringResource(resource = Res.string.saved),
                style = VadretTheme.textStyles.detail,
                color = VadretTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(weight = ACTION_WEIGHT)
            )
        } else {
            OutlinedButton(
                onClick = { onEvent(PlaceIslandEvent.OnSaveClicked) },
                modifier = Modifier.weight(weight = ACTION_WEIGHT)
            ) {
                Text(text = stringResource(resource = Res.string.save_place))
            }
        }
        FilledTonalButton(
            onClick = { onEvent(PlaceIslandEvent.OnOpenForecastClicked) },
            modifier = Modifier.weight(weight = ACTION_WEIGHT)
        ) {
            Text(text = stringResource(resource = Res.string.open_forecast))
        }
    }
}