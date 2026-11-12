package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.first
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.MAXIMUM_FOLLOWED_PLACES
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.pushalert.extension.chosenPlaceName
import se.vadretapp.vadret.pushalert.extension.notifiableFavorites
import se.vadretapp.vadret.pushalert.extension.toGeohashOrNull
import se.vadretapp.vadret.pushalert.model.PlaceName
import se.vadretapp.vadret.pushalert.model.PushAlertChoices

internal class DefaultGetPushAlertPlaceNamesTask(
    private val favoriteRepository: FavoriteRepository,
    private val locationRepository: LocationRepository
) : GetPushAlertPlaceNamesTask {

    override suspend fun invoke(
        currentGeohash: Geohash,
        pushAlertChoices: PushAlertChoices
    ): Either<Failure, Map<Geohash, PlaceName?>> =
        locationRepository
            .observeLocationPreference()
            .first()
            .flatMap { locationPreference: LocationPreference ->
                favoriteRepository.observeFavorites().first().map { favorites: List<Favorite> ->
                    val chosenPlaceNames: List<Pair<Geohash, PlaceName?>> = if (pushAlertChoices.isChosenPlaceNotified) {
                        listOf(currentGeohash to locationPreference.chosenPlaceName()?.toPlaceNameOrNull())
                    } else {
                        emptyList()
                    }
                    val savedPlaceNames: List<Pair<Geohash, PlaceName?>> = locationPreference
                        .notifiableFavorites(favorites = favorites)
                        .filter { favorite: Favorite -> favorite.favoriteId in pushAlertChoices.notifiedFavoriteIds }
                        .mapNotNull { favorite: Favorite -> favorite.toNamedGeohashOrNull() }
                    (chosenPlaceNames + savedPlaceNames)
                        .distinctBy { (geohash: Geohash, _) -> geohash }
                        .take(n = MAXIMUM_FOLLOWED_PLACES)
                        .toMap()
                }
            }
}

private fun Favorite.toNamedGeohashOrNull(): Pair<Geohash, PlaceName?>? {
    val geohash: Geohash = toGeohashOrNull() ?: return null
    return geohash to locality.toPlaceNameOrNull()
}

private fun String.toPlaceNameOrNull(): PlaceName? = PlaceName.parseOrNull(raw = this)