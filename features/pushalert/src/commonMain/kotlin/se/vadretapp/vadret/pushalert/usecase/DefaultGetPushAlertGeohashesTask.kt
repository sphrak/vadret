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
import se.vadretapp.vadret.pushalert.extension.notifiableFavorites
import se.vadretapp.vadret.pushalert.extension.toGeohashOrNull
import se.vadretapp.vadret.pushalert.model.PushAlertChoices

internal class DefaultGetPushAlertGeohashesTask(
    private val favoriteRepository: FavoriteRepository,
    private val locationRepository: LocationRepository
) : GetPushAlertGeohashesTask {

    override suspend fun invoke(
        currentGeohash: Geohash,
        pushAlertChoices: PushAlertChoices
    ): Either<Failure, List<Geohash>> =
        locationRepository
            .observeLocationPreference()
            .first()
            .flatMap { locationPreference: LocationPreference ->
                favoriteRepository.observeFavorites().first().map { favorites: List<Favorite> ->
                    val chosenGeohashes: List<Geohash> = if (pushAlertChoices.isChosenPlaceNotified) {
                        listOf(currentGeohash)
                    } else {
                        emptyList()
                    }
                    val savedGeohashes: List<Geohash> = locationPreference
                        .notifiableFavorites(favorites = favorites)
                        .filter { favorite: Favorite -> favorite.favoriteId in pushAlertChoices.notifiedFavoriteIds }
                        .mapNotNull { favorite: Favorite -> favorite.toGeohashOrNull() }
                    (chosenGeohashes + savedGeohashes)
                        .distinct()
                        .take(n = MAXIMUM_FOLLOWED_PLACES)
                }
            }
}