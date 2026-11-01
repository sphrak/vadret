package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.place.model.Place

internal interface SelectPlaceTask {

    suspend operator fun invoke(place: Place): Either<Failure, Unit>
}