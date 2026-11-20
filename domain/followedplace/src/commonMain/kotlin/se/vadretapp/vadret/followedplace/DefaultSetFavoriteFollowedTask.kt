package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.flatMap
import kotlinx.coroutines.flow.first
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.extension.otherFollowedFavorites
import se.vadretapp.vadret.location.LocationRepository
import se.vadretapp.vadret.location.model.LocationPreference

internal class DefaultSetFavoriteFollowedTask(
    private val favoriteRepository: FavoriteRepository,
    private val locationRepository: LocationRepository
) : SetFavoriteFollowedTask {

    override suspend fun invoke(
        favoriteId: String,
        isFollowed: Boolean
    ): Either<Failure, Unit> =
        if (isFollowed) {
            follow(favoriteId = favoriteId)
        } else {
            locationRepository.setFavoriteFollowed(
                favoriteId = favoriteId,
                isFollowed = false
            )
        }

    private suspend fun follow(favoriteId: String): Either<Failure, Unit> =
        locationRepository
            .observeLocationPreference()
            .first()
            .flatMap { locationPreference: LocationPreference ->
                favoriteRepository
                    .observeFavorites()
                    .first()
                    .flatMap { favorites: List<Favorite> ->
                        if (locationPreference.otherFollowedFavorites(favorites = favorites).size < MAXIMUM_FOLLOWED_PLACES - 1) {
                            locationRepository.setFavoriteFollowed(
                                favoriteId = favoriteId,
                                isFollowed = true
                            )
                        } else {
                            Failure(errorCode = ErrorCode.FOLLOWED_PLACE_LIMIT).asLeft()
                        }
                    }
            }
}