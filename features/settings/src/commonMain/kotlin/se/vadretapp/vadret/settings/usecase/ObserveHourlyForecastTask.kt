package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure

internal interface ObserveHourlyForecastTask {
    operator fun invoke(): Flow<Either<Failure, Boolean>>
}