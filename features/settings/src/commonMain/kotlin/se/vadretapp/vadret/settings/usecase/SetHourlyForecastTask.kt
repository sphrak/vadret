package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface SetHourlyForecastTask {
    suspend operator fun invoke(isHourlyForecastEnabled: Boolean): Either<Failure, Unit>
}