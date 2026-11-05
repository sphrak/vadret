package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.extension.toLocationChoices
import se.vadretapp.vadret.followedplace.model.LocationChoices
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.LocationPreference

internal class DefaultObserveLocationChoicesTask(
    private val favoriteRepository: FavoriteRepository,
    private val locationRepository: LocationRepository
) : ObserveLocationChoicesTask {

    override fun invoke(): Flow<Either<Failure, LocationChoices>> =
        combine(
            flow = favoriteRepository.observeFavorites(),
            flow2 = locationRepository.observeLocationPreference()
        ) { favorites: Either<Failure, List<Favorite>>, locationPreference: Either<Failure, LocationPreference> ->
            favorites.flatMap { favoriteList: List<Favorite> ->
                locationPreference.map { preference: LocationPreference -> preference.toLocationChoices(favorites = favoriteList) }
            }
        }
}