package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.flatMap
import kotlinx.coroutines.flow.first
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.LocationPreference

internal class DefaultGetChosenPlaceTask(
    private val locationRepository: LocationRepository,
    private val favoriteRepository: FavoriteRepository,
    private val chosenPlaceLookup: ChosenPlaceLookup
) : GetChosenPlaceTask {

    override suspend fun invoke(): Either<Failure, FollowedPlace> =
        locationRepository
            .observeLocationPreference()
            .first()
            .flatMap { locationPreference: LocationPreference ->
                favoriteRepository
                    .observeFavorites()
                    .first()
                    .flatMap { favorites: List<Favorite> ->
                        chosenPlaceLookup.chosenPlace(
                            locationPreference = locationPreference,
                            favorites = favorites
                        )
                    }
            }
}