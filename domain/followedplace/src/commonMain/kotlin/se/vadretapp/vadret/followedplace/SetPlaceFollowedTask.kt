package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

interface SetPlaceFollowedTask {

    suspend operator fun invoke(
        favoriteId: String,
        isFollowed: Boolean
    ): Either<Failure, Unit>
}