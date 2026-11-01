package se.vadretapp.vadret.weatherforecast

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.weatherforecast.model.ForecastView
import se.vadretapp.vadret.weatherforecast.model.LatestWarning
import se.vadretapp.vadret.weatherforecast.model.LocationChoices
import se.vadretapp.vadret.weatherforecast.model.SearchState
import se.vadretapp.vadret.weatherforecast.model.WeatherForecast

@Immutable
data class WeatherForecastState(
    val forecast: WeatherForecast = WeatherForecast(),
    val localityName: String? = null,
    val locationChoices: LocationChoices = LocationChoices(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLocationPermissionRequested: Boolean = false,
    val search: SearchState? = null,
    val latestWarning: LatestWarning? = null,
    val forecastView: ForecastView = ForecastView.DAYS,
    val isHourlyForecastEnabled: Boolean = false,
    val errorMessageResource: StringResource? = null
)