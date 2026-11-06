package se.vadretapp.vadret.warning.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.warning.model.FollowedPlaceWarnings

interface ObserveFollowedPlaceWarningsTask {

    operator fun invoke(isRefresh: Boolean): Flow<Either<Failure, FollowedPlaceWarnings>>
}