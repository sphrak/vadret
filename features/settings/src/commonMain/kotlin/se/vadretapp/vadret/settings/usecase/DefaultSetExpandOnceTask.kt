package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastpreference.ForecastPreferenceRepository

internal class DefaultSetExpandOnceTask(
    private val forecastPreferenceRepository: ForecastPreferenceRepository
) : SetExpandOnceTask {

    override suspend fun invoke(isExpandOnceEnabled: Boolean): Either<Failure, Unit> = forecastPreferenceRepository.setExpandOnceEnabled(isEnabled = isExpandOnceEnabled)
}