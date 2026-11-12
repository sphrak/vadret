package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.extension.chosenFavorite
import se.vadretapp.vadret.followedplace.extension.followedFavorites
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.pushalert.model.PushAlertPlace

internal fun LocationPreference.toPushAlertPlaces(favorites: List<Favorite>): List<PushAlertPlace> =
    listOf(PushAlertPlace.ChosenPlace(name = chosenPlaceName())) +
        notifiableFavorites(favorites = favorites).map { favorite: Favorite ->
            PushAlertPlace.SavedPlace(
                favoriteId = favorite.favoriteId,
                name = favorite.locality
            )
        }

internal fun LocationPreference.chosenPlaceName(): String? =
    if (isAutomatic) {
        null
    } else {
        storedLocation.locality
    }

internal fun LocationPreference.notifiableFavorites(favorites: List<Favorite>): List<Favorite> {
    val chosenFavorite: Favorite? = chosenFavorite(favorites = favorites)
    return followedFavorites(favorites = favorites).filter { favorite: Favorite ->
        val geohash: Geohash? = favorite.toGeohashOrNull()
        favorite != chosenFavorite && geohash != null && geohash.isInSwedishWaters()
    }
}