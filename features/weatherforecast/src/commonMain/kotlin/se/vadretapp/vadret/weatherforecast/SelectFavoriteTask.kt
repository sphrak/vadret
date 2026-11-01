package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite

internal interface SelectFavoriteTask {

    suspend operator fun invoke(favorite: Favorite): Either<Failure, Unit>
}