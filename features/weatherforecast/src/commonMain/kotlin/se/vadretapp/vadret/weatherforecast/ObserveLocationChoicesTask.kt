package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.weatherforecast.model.LocationChoices

internal interface ObserveLocationChoicesTask {

    operator fun invoke(): Flow<Either<Failure, LocationChoices>>
}