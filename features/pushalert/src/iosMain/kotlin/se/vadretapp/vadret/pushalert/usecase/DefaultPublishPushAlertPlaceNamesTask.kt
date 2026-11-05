package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.successOrNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.pushalert.PushAlertPlaceNameRepository
import se.vadretapp.vadret.pushalert.extension.toCell
import se.vadretapp.vadret.pushalert.extension.toGeohashOrNull

// The Notification Service Extension names a wind or temperature alert after the saved place in its cell.
// It cannot tell where the phone is, so unlike AddPlaceNameTask it also names a saved place the phone is at.
internal class DefaultPublishPushAlertPlaceNamesTask(
    private val favoriteRepository: FavoriteRepository,
    private val pushAlertPlaceNameRepository: PushAlertPlaceNameRepository
) : PublishPushAlertPlaceNamesTask {
    override suspend fun invoke() {
        favoriteRepository
            .observeFavorites()
            .map { favorites: Either<Failure, List<Favorite>> -> favorites.successOrNull?.placeNamesByCell() }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { placeNamesByCell: Map<String, String> ->
                pushAlertPlaceNameRepository.savePlaceNames(placeNamesByCell = placeNamesByCell)
            }
    }
}

private fun List<Favorite>.placeNamesByCell(): Map<String, String> =
    mapNotNull { favorite: Favorite -> favorite.cellAndLocalityOrNull() }
        .distinctBy { cellAndLocality: Pair<String, String> -> cellAndLocality.first }
        .toMap()

private fun Favorite.cellAndLocalityOrNull(): Pair<String, String>? {
    val geohash = toGeohashOrNull() ?: return null
    return geohash.toCell() to locality
}