package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite

internal interface ObserveFavoritesTask {
    operator fun invoke(): Flow<Either<Failure, List<Favorite>>>
}