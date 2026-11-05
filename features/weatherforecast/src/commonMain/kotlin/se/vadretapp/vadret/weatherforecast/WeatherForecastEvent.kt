package se.vadretapp.vadret.weatherforecast

import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.place.model.Place
import se.vadretapp.vadret.weatherforecast.model.ForecastView

sealed interface WeatherForecastEvent {

    data object OnShown : WeatherForecastEvent
    data class OnPlaceShown(
        val latitude: Double,
        val longitude: Double
    ) : WeatherForecastEvent
    data object OnSavePlaceClicked : WeatherForecastEvent
    data class OnPlaceLabelChanged(val label: String) : WeatherForecastEvent
    data object OnPlaceLabelDismissed : WeatherForecastEvent
    data object OnResumed : WeatherForecastEvent
    data class OnLocaleChanged(val languageTag: String) : WeatherForecastEvent
    data object OnRefresh : WeatherForecastEvent
    data object OnErrorShown : WeatherForecastEvent
    data class OnForecastViewSelected(val forecastView: ForecastView) : WeatherForecastEvent
    data class OnDayToggled(val dayKey: String) : WeatherForecastEvent
    data class OnLocationPermissionResult(val isGranted: Boolean) : WeatherForecastEvent
    data object OnAutomaticLocationClicked : WeatherForecastEvent
    data class OnFavoriteClicked(val favorite: Favorite) : WeatherForecastEvent
    data class OnFavoriteRemoveClicked(val favoriteId: String) : WeatherForecastEvent
    data object OnAddPlaceClicked : WeatherForecastEvent
    data object OnSearchDismissed : WeatherForecastEvent
    data class OnSearchSubmitted(val query: String) : WeatherForecastEvent
    data class OnSearchQueryChanged(val query: String) : WeatherForecastEvent
    data class OnSuggestionClicked(val place: Place) : WeatherForecastEvent
}