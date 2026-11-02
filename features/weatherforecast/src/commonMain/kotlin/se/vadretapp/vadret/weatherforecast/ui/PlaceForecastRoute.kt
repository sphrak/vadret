package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.close
import se.vadretapp.vadret.core.resources.ic_close
import se.vadretapp.vadret.core.resources.not_saved
import se.vadretapp.vadret.core.resources.save
import se.vadretapp.vadret.core.resources.saved
import se.vadretapp.vadret.core.resources.unknown_area
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.weatherforecast.WeatherForecastEvent
import se.vadretapp.vadret.weatherforecast.WeatherForecastState
import se.vadretapp.vadret.weatherforecast.WeatherForecastViewModel
import se.vadretapp.vadret.weatherforecast.model.PlaceNaming

private val saveEndPadding = 13.dp
private const val SINGLE_LINE = 1

@Composable
fun PlaceForecastRoute(
    latitude: Double,
    longitude: Double,
    onClose: () -> Unit,
    onWarningClick: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeatherForecastViewModel = koinViewModel()
) {
    val state: WeatherForecastState by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(latitude, longitude) {
        viewModel.onEvent(
            event = WeatherForecastEvent.OnPlaceShown(
                latitude = latitude,
                longitude = longitude
            )
        )
    }
    PlaceForecastScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onClose = onClose,
        onWarningClick = onWarningClick,
        modifier = modifier
    )
}

@Composable
internal fun PlaceForecastScreen(
    state: WeatherForecastState,
    onEvent: (WeatherForecastEvent) -> Unit,
    onClose: () -> Unit,
    onWarningClick: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val placeNaming: PlaceNaming? = state.placeNaming
    if (placeNaming != null) {
        PlaceLabelDialog(
            placeNaming = placeNaming,
            onLabelChanged = { label: String -> onEvent(WeatherForecastEvent.OnPlaceLabelChanged(label = label)) },
            onSave = { onEvent(WeatherForecastEvent.OnSavePlaceClicked) },
            onDismiss = { onEvent(WeatherForecastEvent.OnPlaceLabelDismissed) }
        )
    }
    ForecastScaffold(
        state = state,
        onEvent = onEvent,
        onWarningClick = onWarningClick,
        topBar = {
            PlaceTopAppBar(
                state = state,
                onClose = onClose,
                onSaveClicked = { onEvent(WeatherForecastEvent.OnSavePlaceClicked) }
            )
        },
        modifier = modifier
    )
}

@Composable
private fun PlaceTopAppBar(
    state: WeatherForecastState,
    onClose: () -> Unit,
    onSaveClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isShown: Boolean = state.forecast.current != null
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(resource = Res.drawable.ic_close),
                    contentDescription = stringResource(resource = Res.string.close)
                )
            }
        },
        title = {
            if (isShown) {
                PlaceTitle(
                    localityName = state.localityName,
                    isPlaceSaved = state.isPlaceSaved
                )
            }
        },
        actions = {
            if (isShown && !state.isPlaceSaved) {
                FilledTonalButton(
                    onClick = onSaveClicked,
                    modifier = Modifier.padding(end = saveEndPadding)
                ) {
                    Text(text = stringResource(resource = Res.string.save))
                }
            }
        },
        expandedHeight = TopAppBarDefaults.TopAppBarExpandedHeight,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = VadretTheme.colorScheme.background,
            titleContentColor = VadretTheme.colorScheme.onBackground,
            navigationIconContentColor = VadretTheme.colorScheme.onBackground
        ),
        modifier = modifier
    )
}

@Composable
private fun PlaceTitle(
    localityName: String?,
    isPlaceSaved: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = localityName ?: stringResource(resource = Res.string.unknown_area),
            style = VadretTheme.textStyles.title,
            maxLines = SINGLE_LINE,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(
                resource = if (isPlaceSaved) {
                    Res.string.saved
                } else {
                    Res.string.not_saved
                }
            ),
            style = VadretTheme.textStyles.detailLabel,
            color = VadretTheme.colorScheme.onSurfaceVariant
        )
    }
}