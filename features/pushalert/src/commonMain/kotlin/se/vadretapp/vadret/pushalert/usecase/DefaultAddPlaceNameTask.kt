package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.first
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.pushalert.PushAlertRepository
import se.vadretapp.vadret.pushalert.extension.toGeohashOrNull
import se.vadretapp.vadret.pushalert.model.PushAlertKind
import se.vadretapp.vadret.pushalert.model.PushAlertMessage

internal class DefaultAddPlaceNameTask(
    private val favoriteRepository: FavoriteRepository,
    private val pushAlertRepository: PushAlertRepository
) : AddPlaceNameTask {
    override suspend fun invoke(
        pushAlertMessage: PushAlertMessage,
        favoriteIds: Set<String>
    ): PushAlertMessage {
        val cell = pushAlertMessage.cell
        if (cell == null || favoriteIds.isEmpty() || !pushAlertMessage.kind.isAboutOneCell()) {
            return pushAlertMessage
        }
        val placeName = chosenPlaceNameIn(
            cell = cell,
            favoriteIds = favoriteIds
        )
        if (placeName == null || isWhereThePhoneIs(cell = cell)) {
            return pushAlertMessage
        }
        return pushAlertMessage.copy(title = "${pushAlertMessage.title}$PLACE_NAME_SEPARATOR$placeName")
    }

    private suspend fun chosenPlaceNameIn(
        cell: String,
        favoriteIds: Set<String>
    ): String? =
        when (val favorites = favoriteRepository.observeFavorites().first()) {
            is Either.Left -> null
            is Either.Right -> favorites.b.firstOrNull { favorite: Favorite ->
                favorite.favoriteId in favoriteIds && favorite.toGeohashOrNull()?.toCell() == cell
            }?.locality
        }

    private suspend fun isWhereThePhoneIs(cell: String): Boolean =
        when (val syncedCurrentGeohash = pushAlertRepository.syncedCurrentGeohash()) {
            is Either.Left -> false
            is Either.Right -> syncedCurrentGeohash.b?.toCell() == cell
        }
}

private fun PushAlertKind.isAboutOneCell(): Boolean =
    when (this) {
        PushAlertKind.WARNING,
        PushAlertKind.POLLEN -> false
        PushAlertKind.TEMPERATURE,
        PushAlertKind.WIND -> true
    }

private fun Geohash.toCell(): String = value.take(n = CELL_LENGTH)

private const val CELL_LENGTH = 4
private const val PLACE_NAME_SEPARATOR = " - "