package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.flatMap
import kotlinx.coroutines.flow.first
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.FavoriteRepository
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.location.LocationRepository

internal class DefaultSetLocationModeTask(
    private val locationRepository: LocationRepository,
    private val favoriteRepository: FavoriteRepository
) : SetLocationModeTask {

    // A position followed before the limit existed stays followed, so only a switch to the position is checked.
    override suspend fun invoke(isAutomatic: Boolean): Either<Failure, Unit> =
        if (isAutomatic) {
            locationRepository.isAutomatic().flatMap { isFollowingPosition: Boolean ->
                if (isFollowingPosition) {
                    Unit.asRight()
                } else {
                    followPosition()
                }
            }
        } else {
            locationRepository.setAutomatic(isAutomatic = false)
        }

    // The position is followed as a place of its own, so it only fits next to fewer saved places than the limit.
    private suspend fun followPosition(): Either<Failure, Unit> =
        favoriteRepository
            .observeFavorites()
            .first()
            .flatMap { favorites: List<Favorite> ->
                if (favorites.size < MAXIMUM_FOLLOWED_PLACES) {
                    locationRepository.setAutomatic(isAutomatic = true)
                } else {
                    Failure(errorCode = ErrorCode.FOLLOWED_PLACE_LIMIT).asLeft()
                }
            }
}