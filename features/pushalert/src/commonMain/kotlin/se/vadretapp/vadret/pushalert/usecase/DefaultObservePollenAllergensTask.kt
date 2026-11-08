package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollenpreference.PollenPreferenceRepository
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences
import se.vadretapp.vadret.pushalert.extension.notifiedAllergenIds

internal class DefaultObservePollenAllergensTask(
    private val pollenPreferenceRepository: PollenPreferenceRepository
) : ObservePollenAllergensTask {
    override fun invoke(): Flow<Either<Failure, Set<String>>> =
        pollenPreferenceRepository
            .observePollenPreferences()
            .map { pollenPreferences: Either<Failure, PollenPreferences> ->
                pollenPreferences.map { preferences: PollenPreferences -> preferences.notifiedAllergenIds() }
            }
}