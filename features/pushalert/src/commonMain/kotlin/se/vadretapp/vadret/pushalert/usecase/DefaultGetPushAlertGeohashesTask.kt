package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.first
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.MAXIMUM_FOLLOWED_PLACES
import se.vadretapp.vadret.followedplace.extension.followedFavorites
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.pushalert.extension.toGeohashOrNull

internal class DefaultGetPushAlertGeohashesTask(
    private val favoriteRepository: FavoriteRepository,
    private val locationRepository: LocationRepository
) : GetPushAlertGeohashesTask {

    // The same saved places as for the warnings are followed, after where the phone is and only in Sweden.
    override suspend fun invoke(currentGeohash: Geohash): Either<Failure, List<Geohash>> =
        locationRepository
            .observeLocationPreference()
            .first()
            .flatMap { locationPreference: LocationPreference ->
                favoriteRepository.observeFavorites().first().map { favorites: List<Favorite> ->
                    (listOf(currentGeohash) + locationPreference.followedFavorites(favorites = favorites).geohashesInSweden())
                        .distinct()
                        .take(n = MAXIMUM_FOLLOWED_PLACES)
                }
            }
}

private fun List<Favorite>.geohashesInSweden(): List<Geohash> =
    mapNotNull { favorite: Favorite -> favorite.toGeohashOrNull() }
        .filter { geohash: Geohash -> geohash.isInSwedishWaters() }