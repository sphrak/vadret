package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastpreference.ForecastPreferenceRepository

internal class DefaultSetHourlyForecastTask(
    private val forecastPreferenceRepository: ForecastPreferenceRepository
) : SetHourlyForecastTask {

    override suspend fun invoke(isHourlyForecastEnabled: Boolean): Either<Failure, Unit> = forecastPreferenceRepository.setHourlyForecastEnabled(isEnabled = isHourlyForecastEnabled)
}