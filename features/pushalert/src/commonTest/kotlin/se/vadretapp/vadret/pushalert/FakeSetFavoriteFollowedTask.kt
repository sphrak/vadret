package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.SetFavoriteFollowedTask

internal class FakeSetFavoriteFollowedTask : SetFavoriteFollowedTask {
    val followed = mutableListOf<Pair<String, Boolean>>()

    override suspend fun invoke(
        favoriteId: String,
        isFollowed: Boolean
    ): Either<Failure, Unit> {
        followed += favoriteId to isFollowed
        return Unit.asRight()
    }
}