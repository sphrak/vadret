package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.first
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.pushalert.extension.toGeohashOrNull

internal class DefaultGetPushAlertGeohashesTask(
    private val favoriteRepository: FavoriteRepository
) : GetPushAlertGeohashesTask {
    override suspend fun invoke(
        currentGeohash: Geohash,
        favoriteIds: Set<String>
    ): Either<Failure, List<Geohash>> =
        favoriteRepository.observeFavorites().first().map { favorites: List<Favorite> ->
            (listOf(currentGeohash) + favorites.chosenGeohashesInSweden(favoriteIds = favoriteIds))
                .distinct()
                .take(n = MAXIMUM_GEOHASHES)
        }
}

private fun List<Favorite>.chosenGeohashesInSweden(favoriteIds: Set<String>): List<Geohash> =
    filter { favorite: Favorite -> favorite.favoriteId in favoriteIds }
        .mapNotNull { favorite: Favorite -> favorite.toGeohashOrNull() }
        .filter { geohash: Geohash -> geohash.isInSwedishWaters() }

private const val MAXIMUM_GEOHASHES = 5