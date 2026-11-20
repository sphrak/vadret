package se.vadretapp.vadret.followedplace.extension

import kotlinx.collections.immutable.toImmutableList
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.MAXIMUM_FOLLOWED_PLACES
import se.vadretapp.vadret.followedplace.model.FavoriteChoice
import se.vadretapp.vadret.followedplace.model.LocationChoices
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation

internal fun LocationPreference.toLocationChoices(favorites: List<Favorite>): LocationChoices {
    val chosenFavorite: Favorite? = chosenFavorite(favorites = favorites)
    val followedFavorites: List<Favorite> = followedFavorites(favorites = favorites)
    return LocationChoices(
        isAutomatic = isAutomatic,
        chosenPlaceName = if (isAutomatic) {
            null
        } else {
            storedLocation.locality
        },
        favorites = favorites
            .map { favorite: Favorite ->
                FavoriteChoice(
                    favorite = favorite,
                    isSelected = favorite == chosenFavorite,
                    isFollowed = favorite in followedFavorites
                )
            }.toImmutableList()
    )
}

fun LocationPreference.chosenFavorite(favorites: List<Favorite>): Favorite? =
    if (isAutomatic) {
        null
    } else {
        favorites.firstOrNull { favorite: Favorite -> favorite.isAt(storedLocation = storedLocation) }
    }

// The chosen place is always followed, and another saved place only once it is asked for.
fun LocationPreference.followedFavorites(favorites: List<Favorite>): List<Favorite> {
    val chosenFavorite: Favorite? = chosenFavorite(favorites = favorites)
    return listOfNotNull(chosenFavorite) + otherFollowedFavorites(favorites = favorites)
}

internal fun LocationPreference.otherFollowedFavorites(favorites: List<Favorite>): List<Favorite> {
    val chosenFavorite: Favorite? = chosenFavorite(favorites = favorites)
    return favorites
        .filter { favorite: Favorite -> favorite != chosenFavorite && favorite.favoriteId in followedFavoriteIds }
        .take(n = MAXIMUM_FOLLOWED_PLACES - 1)
}

// A chosen place that is no saved place, like the position, takes the room of one. Saving the chosen place itself
// fills that room, so it fits as well.
internal fun LocationPreference.maximumFavorites(
    favorites: List<Favorite>,
    locality: String,
    municipality: String,
    county: String
): Int {
    val isChosenPlaceSaved: Boolean = !isAutomatic &&
        storedLocation.isPlace(
            locality = locality,
            municipality = municipality,
            county = county
        )
    return if (chosenFavorite(favorites = favorites) != null || isChosenPlaceSaved) {
        MAXIMUM_FOLLOWED_PLACES
    } else {
        MAXIMUM_FOLLOWED_PLACES - 1
    }
}

private fun Favorite.isAt(storedLocation: StoredLocation): Boolean =
    storedLocation.isPlace(
        locality = locality,
        municipality = municipality,
        county = county
    )

private fun StoredLocation.isPlace(
    locality: String,
    municipality: String,
    county: String
): Boolean = this.locality == locality && this.municipality == municipality && this.county == county