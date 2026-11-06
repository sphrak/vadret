package se.vadretapp.vadret.radar.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollenpreference.PollenPreferenceRepository
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences

internal class DefaultObservePollenPreferencesTask(
    private val pollenPreferenceRepository: PollenPreferenceRepository
) : ObservePollenPreferencesTask {

    override fun invoke(): Flow<Either<Failure, PollenPreferences>> = pollenPreferenceRepository.observePollenPreferences()
}