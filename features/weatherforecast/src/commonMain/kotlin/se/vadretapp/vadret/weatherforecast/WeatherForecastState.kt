package se.vadretapp.vadret.weatherforecast

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.followedplace.model.LocationChoices
import se.vadretapp.vadret.weatherforecast.model.ForecastView
import se.vadretapp.vadret.weatherforecast.model.LatestWarning
import se.vadretapp.vadret.weatherforecast.model.PlaceNaming
import se.vadretapp.vadret.weatherforecast.model.WeatherForecast

@Immutable
data class WeatherForecastState(
    val forecast: WeatherForecast = WeatherForecast(),
    val localityName: String? = null,
    val outdatedForecastTimeText: String? = null,
    val locationChoices: LocationChoices = LocationChoices(
        isAutomatic = true,
        chosenPlaceName = null,
        favorites = persistentListOf()
    ),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLocationPermissionRequested: Boolean = false,
    val latestWarning: LatestWarning? = null,
    val forecastView: ForecastView = ForecastView.DAYS,
    val isHourlyForecastEnabled: Boolean = false,
    val isPlaceSaved: Boolean = false,
    val placeNaming: PlaceNaming? = null,
    val errorMessageResource: StringResource? = null
)