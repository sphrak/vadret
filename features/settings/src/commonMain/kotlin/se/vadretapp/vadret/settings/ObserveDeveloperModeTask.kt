package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure

internal interface ObserveDeveloperModeTask {
    operator fun invoke(): Flow<Either<Failure, Boolean>>
}