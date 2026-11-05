package se.vadretapp.vadret.pollenseason

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure

interface ObservePollenInSeasonTask {
    operator fun invoke(): Flow<Either<Failure, Boolean>>
}