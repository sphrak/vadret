package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.LocalLanguageTag
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretProgressBar
import se.vadretapp.vadret.ui.VadretPullToRefreshBox
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.VadretTopAppBar
import se.vadretapp.vadret.weatherforecast.WeatherForecastEvent
import se.vadretapp.vadret.weatherforecast.WeatherForecastState
import se.vadretapp.vadret.weatherforecast.WeatherForecastViewModel
import se.vadretapp.vadret.weatherforecast.model.CurrentWeather
import se.vadretapp.vadret.weatherforecast.model.ForecastView
import se.vadretapp.vadret.weatherforecast.model.LatestWarning
import se.vadretapp.vadret.weatherforecast.model.SearchState

private const val SEARCH_KEY = "search"
private const val CURRENT_WEATHER_KEY = "current-weather"
private const val FORECAST_VIEW_KEY = "forecast-view"
private const val FORECAST_CONTENT_KEY = "forecast-content"

private val locationEndPadding = 9.dp
private val contentHorizontalPadding = 13.dp
private val contentBottomPadding = 21.dp
private val contentSpacing = 13.dp

@Composable
fun WeatherForecastRoute(
    onWarningClick: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeatherForecastViewModel = koinViewModel()
) {
    val state: WeatherForecastState by viewModel.state.collectAsStateWithLifecycle()
    WeatherForecastScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onWarningClick = onWarningClick,
        modifier = modifier
    )
}

@Composable
internal fun WeatherForecastScreen(
    state: WeatherForecastState,
    onEvent: (WeatherForecastEvent) -> Unit,
    onWarningClick: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val languageTag: String = LocalLanguageTag.current
    LaunchedEffect(languageTag) {
        onEvent(WeatherForecastEvent.OnLocaleChanged(languageTag = languageTag))
    }
    LaunchedEffect(Unit) {
        onEvent(WeatherForecastEvent.OnShown)
    }
    LifecycleResumeEffect(Unit) {
        onEvent(WeatherForecastEvent.OnResumed)
        onPauseOrDispose {}
    }
    LocationPermissionEffect(
        isRequested = state.isLocationPermissionRequested,
        onResult = { isGranted: Boolean ->
            onEvent(WeatherForecastEvent.OnLocationPermissionResult(isGranted = isGranted))
        }
    )

    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val errorMessageResource: StringResource? = state.errorMessageResource
    if (errorMessageResource != null) {
        val message: String = stringResource(resource = errorMessageResource)
        LaunchedEffect(errorMessageResource) {
            snackbarHostState.showSnackbar(message = message)
            onEvent(WeatherForecastEvent.OnErrorShown)
        }
    }

    val search: SearchState? = state.search
    val currentWeather: CurrentWeather? = state.forecast.current
    Scaffold(
        modifier = modifier,
        topBar = {
            VadretTopAppBar(
                actions = {
                    LocationPicker(
                        modifier = Modifier.padding(end = locationEndPadding),
                        localityName = state.localityName,
                        locationChoices = state.locationChoices,
                        onEvent = onEvent
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = VadretTheme.colorScheme.background,
        contentWindowInsets = WindowInsets()
    ) { innerPadding: PaddingValues ->
        VadretPullToRefreshBox(
            modifier = Modifier
                .padding(paddingValues = innerPadding)
                .fillMaxSize(),
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(WeatherForecastEvent.OnRefresh) }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = contentHorizontalPadding,
                    end = contentHorizontalPadding,
                    bottom = contentBottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(space = contentSpacing)
            ) {
                if (search != null) {
                    item(
                        key = SEARCH_KEY,
                        contentType = SEARCH_KEY
                    ) {
                        SearchPanel(
                            modifier = Modifier.animateItem(),
                            search = search,
                            onEvent = onEvent
                        )
                    }
                } else if (currentWeather != null) {
                    item(
                        key = CURRENT_WEATHER_KEY,
                        contentType = CURRENT_WEATHER_KEY
                    ) {
                        CurrentWeather(
                            modifier = Modifier.animateItem(),
                            currentWeather = currentWeather,
                            latestWarning = state.latestWarning,
                            onWarningClick = onWarningClick
                        )
                    }
                }
                forecast(
                    state = state,
                    onEvent = onEvent
                )
            }
            if (state.isLoading) {
                VadretProgressBar(modifier = Modifier.align(alignment = Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun CurrentWeather(
    currentWeather: CurrentWeather,
    latestWarning: LatestWarning?,
    onWarningClick: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(space = contentSpacing),
        modifier = modifier
    ) {
        if (latestWarning != null) {
            WarningPill(
                latestWarning = latestWarning,
                onClick = { onWarningClick(latestWarning.warningIdentifiers) }
            )
        }
        CurrentWeatherCard(currentWeather = currentWeather)
    }
}

private fun LazyListScope.forecast(
    state: WeatherForecastState,
    onEvent: (WeatherForecastEvent) -> Unit
) {
    if (state.forecast.days.isEmpty()) {
        return
    }
    if (state.isHourlyForecastEnabled) {
        item(
            key = FORECAST_VIEW_KEY,
            contentType = FORECAST_VIEW_KEY
        ) {
            ForecastViewSelector(
                modifier = Modifier.animateItem(),
                forecastView = state.forecastView,
                onForecastViewSelected = { forecastView: ForecastView ->
                    onEvent(WeatherForecastEvent.OnForecastViewSelected(forecastView = forecastView))
                }
            )
        }
    }
    item(
        key = FORECAST_CONTENT_KEY,
        contentType = FORECAST_CONTENT_KEY
    ) {
        ForecastContent(
            modifier = Modifier.animateItem(),
            forecastView = state.forecastView,
            forecast = state.forecast,
            onEvent = onEvent
        )
    }
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
            onWarningClick = {}
        )
    }
}