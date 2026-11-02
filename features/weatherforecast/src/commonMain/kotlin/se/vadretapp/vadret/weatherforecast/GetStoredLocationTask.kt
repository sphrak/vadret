package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.model.StoredLocation

internal interface GetStoredLocationTask {

    suspend operator fun invoke(): Either<Failure, StoredLocation>
}