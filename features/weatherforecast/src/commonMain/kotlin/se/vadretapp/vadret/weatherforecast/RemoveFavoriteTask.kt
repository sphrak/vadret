package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface RemoveFavoriteTask {

    suspend operator fun invoke(favoriteId: String): Either<Failure, Unit>
}