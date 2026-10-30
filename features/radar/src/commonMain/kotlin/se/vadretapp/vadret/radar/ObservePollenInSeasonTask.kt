package se.vadretapp.vadret.radar

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure

internal interface ObservePollenInSeasonTask {
    operator fun invoke(): Flow<Either<Failure, Boolean>>
}