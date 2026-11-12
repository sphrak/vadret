package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.LocationPermissionEffect
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.VadretTopAppBar
import se.vadretapp.vadret.weatherforecast.WeatherForecastEvent
import se.vadretapp.vadret.weatherforecast.WeatherForecastState
import se.vadretapp.vadret.weatherforecast.WeatherForecastViewModel

private val locationEndPadding = 9.dp

@Composable
fun WeatherForecastRoute(
    onWarningClick: (List<String>) -> Unit,
    onAddPlaceClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeatherForecastViewModel = koinViewModel()
) {
    val state: WeatherForecastState by viewModel.state.collectAsStateWithLifecycle()

    WeatherForecastScreen(
        modifier = modifier,
        state = state,
        onEvent = viewModel::onEvent,
        onWarningClick = onWarningClick,
        onAddPlaceClick = onAddPlaceClick
    )
}

@Composable
internal fun WeatherForecastScreen(
    state: WeatherForecastState,
    onEvent: (WeatherForecastEvent) -> Unit,
    onWarningClick: (List<String>) -> Unit,
    onAddPlaceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onEvent(WeatherForecastEvent.OnShown)
    }

    LocationPermissionEffect(
        isRequested = state.isLocationPermissionRequested,
        onResult = { isGranted: Boolean ->
            onEvent(WeatherForecastEvent.OnLocationPermissionResult(isGranted = isGranted))
        }
    )

    ForecastScaffold(
        modifier = modifier,
        onEvent = onEvent,
        state = state,
        onWarningClick = onWarningClick,
        topBar = {
            VadretTopAppBar(
                actions = {
                    LocationPicker(
                        localityName = state.localityName,
                        locationChoices = state.locationChoices,
                        onEvent = onEvent,
                        onAddPlaceClick = onAddPlaceClick
                    )
                }
            )
        }
    )
}

@Preview(device = Devices.PHONE)
@Composable
private fun WeatherForecastScreenPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        WeatherForecastScreen(
            state = weatherForecastPreviewState(),
            onEvent = {},
            onWarningClick = {},
            onAddPlaceClick = {}
        )
    }
}