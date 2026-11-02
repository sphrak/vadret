package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.pushalert.PushAlertPlaceNameRepository
import se.vadretapp.vadret.pushalert.extension.toCell
import se.vadretapp.vadret.pushalert.extension.toGeohashOrNull

// The Notification Service Extension names a wind or temperature alert after the followed saved place in its cell.
// It cannot tell where the phone is, so unlike AddPlaceNameTask it also names a saved place the phone is at.
internal class DefaultPublishPushAlertPlaceNamesTask(
    private val favoriteRepository: FavoriteRepository,
    private val locationRepository: LocationRepository,
    private val pushAlertPlaceNameRepository: PushAlertPlaceNameRepository
) : PublishPushAlertPlaceNamesTask {
    override suspend fun invoke() {
        combine(
            favoriteRepository.observeFavorites(),
            locationRepository.observeLocationPreference()
        ) { favorites: Either<Failure, List<Favorite>>, locationPreference: Either<Failure, LocationPreference> ->
            placeNamesByCell(
                favorites = favorites,
                locationPreference = locationPreference
            )
        }.filterNotNull()
            .distinctUntilChanged()
            .collect { placeNamesByCell: Map<String, String> ->
                pushAlertPlaceNameRepository.savePlaceNames(placeNamesByCell = placeNamesByCell)
            }
    }

    private fun placeNamesByCell(
        favorites: Either<Failure, List<Favorite>>,
        locationPreference: Either<Failure, LocationPreference>
    ): Map<String, String>? =
        when {
            favorites is Either.Right && locationPreference is Either.Right -> favorites.b.placeNamesByCell(favoriteIds = locationPreference.b.followedFavoriteIds)
            else -> null
        }
}

private fun List<Favorite>.placeNamesByCell(favoriteIds: Set<String>): Map<String, String> =
    filter { favorite: Favorite -> favorite.favoriteId in favoriteIds }
        .mapNotNull { favorite: Favorite -> favorite.cellAndLocalityOrNull() }
        .distinctBy { cellAndLocality: Pair<String, String> -> cellAndLocality.first }
        .toMap()

private fun Favorite.cellAndLocalityOrNull(): Pair<String, String>? {
    val geohash = toGeohashOrNull() ?: return null
    return geohash.toCell() to locality
}