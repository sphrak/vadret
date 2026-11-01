package se.vadretapp.vadret.weatherforecast.extension

import kotlinx.collections.immutable.toImmutableList
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation
import se.vadretapp.vadret.weatherforecast.model.FavoriteChoice
import se.vadretapp.vadret.weatherforecast.model.LocationChoices

internal fun LocationPreference.toLocationChoices(favorites: List<Favorite>): LocationChoices =
    LocationChoices(
        isAutomatic = isAutomatic,
        favorites = favorites
            .map { favorite: Favorite ->
                FavoriteChoice(
                    favorite = favorite,
                    isSelected = !isAutomatic && favorite.isAt(storedLocation = storedLocation)
                )
            }.toImmutableList()
    )

private fun Favorite.isAt(storedLocation: StoredLocation): Boolean =
    locality == storedLocation.locality &&
        municipality == storedLocation.municipality &&
        county == storedLocation.county