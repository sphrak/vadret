package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.LocationRepository

internal class DefaultSetPlaceFollowedTask(
    private val locationRepository: LocationRepository
) : SetPlaceFollowedTask {

    override suspend fun invoke(
        favoriteId: String,
        isFollowed: Boolean
    ): Either<Failure, Unit> =
        if (isFollowed) {
            locationRepository
                .followFavorite(
                    favoriteId = favoriteId,
                    maximumFollowedFavorites = MAXIMUM_FOLLOWED_FAVORITES
                ).flatMap { isNowFollowed: Boolean ->
                    if (isNowFollowed) {
                        Unit.asRight()
                    } else {
                        Failure(errorCode = ErrorCode.FOLLOWED_PLACE_LIMIT).asLeft()
                    }
                }
        } else {
            locationRepository.unfollowFavorite(favoriteId = favoriteId)
        }
}