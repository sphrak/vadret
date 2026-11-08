package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.first
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.pushalert.PushAlertRepository
import se.vadretapp.vadret.pushalert.extension.isIn
import se.vadretapp.vadret.pushalert.extension.toGeohashOrNull
import se.vadretapp.vadret.pushalert.model.PushAlertKind
import se.vadretapp.vadret.pushalert.model.PushAlertMessage

internal class DefaultAddPlaceNameTask(
    private val favoriteRepository: FavoriteRepository,
    private val pushAlertRepository: PushAlertRepository
) : AddPlaceNameTask {
    override suspend fun invoke(pushAlertMessage: PushAlertMessage): PushAlertMessage {
        val cell: String? = pushAlertMessage.cell
        if (cell == null || !pushAlertMessage.kind.isAboutOneCell()) {
            return pushAlertMessage
        }
        val placeName: String? = savedPlaceNameIn(cell = cell)
        if (placeName == null || isWhereThePhoneIs(cell = cell)) {
            return pushAlertMessage
        }
        return pushAlertMessage.copy(title = "${pushAlertMessage.title}$PLACE_NAME_SEPARATOR$placeName")
    }

    private suspend fun savedPlaceNameIn(cell: String): String? =
        when (val favorites: Either<Failure, List<Favorite>> = favoriteRepository.observeFavorites().first()) {
            is Either.Left -> null
            is Either.Right -> favorites.b.firstOrNull { favorite: Favorite ->
                favorite.toGeohashOrNull()?.isIn(cell = cell) == true
            }?.locality
        }

    private suspend fun isWhereThePhoneIs(cell: String): Boolean =
        when (val syncedCurrentGeohash = pushAlertRepository.syncedCurrentGeohash()) {
            is Either.Left -> false
            is Either.Right -> syncedCurrentGeohash.b?.isIn(cell = cell) == true
        }
}

private fun PushAlertKind.isAboutOneCell(): Boolean =
    when (this) {
        PushAlertKind.WARNING,
        PushAlertKind.POLLEN -> false
        PushAlertKind.TEMPERATURE,
        PushAlertKind.WIND,
        PushAlertKind.SLIPPERINESS,
        PushAlertKind.AURORA -> true
    }

private const val PLACE_NAME_SEPARATOR = " - "