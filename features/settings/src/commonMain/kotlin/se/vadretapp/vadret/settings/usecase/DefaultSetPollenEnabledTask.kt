package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollenpreference.PollenPreferenceRepository

internal class DefaultSetPollenEnabledTask(
    private val pollenPreferenceRepository: PollenPreferenceRepository
) : SetPollenEnabledTask {

    override suspend fun invoke(isPollenEnabled: Boolean): Either<Failure, Unit> = pollenPreferenceRepository.setPollenEnabled(isEnabled = isPollenEnabled)
}