package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.LocalHourFormat
import se.vadretapp.vadret.ui.LocalLanguageTag
import se.vadretapp.vadret.ui.LocalScrollBottomInset
import se.vadretapp.vadret.ui.VadretProgressBar
import se.vadretapp.vadret.ui.VadretPullToRefreshBox
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.ic_info
import se.vadretapp.vadret.ui.resources.showing_saved_forecast
import se.vadretapp.vadret.weatherforecast.WeatherForecastEvent
import se.vadretapp.vadret.weatherforecast.WeatherForecastState
import se.vadretapp.vadret.weatherforecast.model.CurrentWeather
import se.vadretapp.vadret.weatherforecast.model.ForecastView
import se.vadretapp.vadret.weatherforecast.model.LatestWarning

private const val CURRENT_WEATHER_KEY = "current-weather"
private const val FORECAST_VIEW_KEY = "forecast-view"
private const val FORECAST_CONTENT_KEY = "forecast-content"

private val contentHorizontalPadding = 13.dp
private val contentBottomPadding = 21.dp
private val contentSpacing = 13.dp

@Composable
internal fun ForecastScaffold(
    state: WeatherForecastState,
    onEvent: (WeatherForecastEvent) -> Unit,
    onWarningClick: (List<String>) -> Unit,
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val languageTag: String = LocalLanguageTag.current
    val hourFormat: HourFormat = LocalHourFormat.current
    LaunchedEffect(languageTag, hourFormat) {
        onEvent(
            WeatherForecastEvent.OnLocaleChanged(
                languageTag = languageTag,
                hourFormat = hourFormat
            )
        )
    }
    LifecycleResumeEffect(Unit) {
        onEvent(WeatherForecastEvent.OnResumed)
        onPauseOrDispose {}
    }

    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val errorMessageResource: StringResource? = state.errorMessageResource
    if (errorMessageResource != null) {
        val message: String = stringResource(resource = errorMessageResource)
        LaunchedEffect(errorMessageResource) {
            snackbarHostState.showSnackbar(message = message)
            onEvent(WeatherForecastEvent.OnErrorShown)
        }
    }

    val currentWeather: CurrentWeather? = state.forecast.current
    val scrollBottomInset: Dp = LocalScrollBottomInset.current
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = scrollBottomInset)
            )
        },
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
                    bottom = contentBottomPadding + scrollBottomInset
                ),
                verticalArrangement = Arrangement.spacedBy(space = contentSpacing)
            ) {
                if (currentWeather != null) {
                    item(
                        key = CURRENT_WEATHER_KEY,
                        contentType = CURRENT_WEATHER_KEY
                    ) {
                        CurrentWeather(
                            modifier = Modifier.animateItem(),
                            currentWeather = currentWeather,
                            latestWarning = state.latestWarning,
                            outdatedForecastTimeText = state.outdatedForecastTimeText,
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
    outdatedForecastTimeText: String?,
    onWarningClick: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(space = contentSpacing),
        modifier = modifier
    ) {
        if (outdatedForecastTimeText != null) {
            OutdatedForecastNote(timeText = outdatedForecastTimeText)
        }
        if (latestWarning != null) {
            WarningPill(
                latestWarning = latestWarning,
                onClick = { onWarningClick(latestWarning.warningIdentifiers) }
            )
        }
        CurrentWeatherCard(currentWeather = currentWeather)
    }
}

@Composable
private fun OutdatedForecastNote(
    timeText: String,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            painter = painterResource(resource = Res.drawable.ic_info),
            contentDescription = null,
            tint = VadretTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(
                resource = Res.string.showing_saved_forecast,
                timeText
            ),
            style = VadretTheme.textStyles.label,
            color = VadretTheme.colorScheme.onSurfaceVariant
        )
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