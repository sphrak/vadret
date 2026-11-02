package se.vadretapp.vadret.weatherforecast

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.weatherforecast.model.CurrentWeather
import se.vadretapp.vadret.weatherforecast.model.PlaceNaming

internal data class PlaceIslandState(
    val location: Location? = null,
    val placeName: String? = null,
    val municipality: String? = null,
    val county: String? = null,
    val currentWeather: CurrentWeather? = null,
    val isLoading: Boolean = false,
    val placeNaming: PlaceNaming? = null,
    val savedFavorite: Favorite? = null,
    val opening: PlaceOpening? = null,
    val errorMessageResource: StringResource? = null
) {

    val isSaved: Boolean = savedFavorite != null

    val shownPlaceName: String? = savedFavorite?.locality ?: placeName
}