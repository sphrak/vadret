package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastpreference.ForecastPreferenceRepository

internal class DefaultSetAuroraTask(
    private val forecastPreferenceRepository: ForecastPreferenceRepository
) : SetAuroraTask {

    override suspend fun invoke(isAuroraEnabled: Boolean): Either<Failure, Unit> = forecastPreferenceRepository.setAuroraEnabled(isEnabled = isAuroraEnabled)
}