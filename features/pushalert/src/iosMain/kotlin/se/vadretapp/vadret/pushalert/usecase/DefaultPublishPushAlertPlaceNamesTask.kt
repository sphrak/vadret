package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.pushalert.PushAlertPlaceNameRepository
import se.vadretapp.vadret.pushalert.PushAlertRepository
import se.vadretapp.vadret.pushalert.extension.toCell
import se.vadretapp.vadret.pushalert.extension.toGeohashOrNull
import se.vadretapp.vadret.pushalert.model.PushAlertChoices

// The Notification Service Extension names a wind or temperature alert after the chosen saved place in its cell.
// It cannot tell where the phone is, so unlike AddPlaceNameTask it also names a saved place the phone is at.
internal class DefaultPublishPushAlertPlaceNamesTask(
    private val favoriteRepository: FavoriteRepository,
    private val pushAlertRepository: PushAlertRepository,
    private val pushAlertPlaceNameRepository: PushAlertPlaceNameRepository
) : PublishPushAlertPlaceNamesTask {
    override suspend fun invoke() {
        combine(
            favoriteRepository.observeFavorites(),
            pushAlertRepository.observeChoices()
        ) { favorites: Either<Failure, List<Favorite>>, pushAlertChoices: Either<Failure, PushAlertChoices> ->
            placeNamesByCell(
                favorites = favorites,
                pushAlertChoices = pushAlertChoices
            )
        }.filterNotNull()
            .distinctUntilChanged()
            .collect { placeNamesByCell: Map<String, String> ->
                pushAlertPlaceNameRepository.savePlaceNames(placeNamesByCell = placeNamesByCell)
            }
    }

    private fun placeNamesByCell(
        favorites: Either<Failure, List<Favorite>>,
        pushAlertChoices: Either<Failure, PushAlertChoices>
    ): Map<String, String>? =
        when {
            favorites is Either.Right && pushAlertChoices is Either.Right -> favorites.b.placeNamesByCell(favoriteIds = pushAlertChoices.b.favoriteIds)
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