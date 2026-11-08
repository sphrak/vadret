package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.settings.model.Labs

internal interface ObserveLabsTask {
    operator fun invoke(): Flow<Either<Failure, Labs>>
}