package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollenpreference.PollenPreferenceRepository

internal class DefaultSetPollenAllergenTask(
    private val pollenPreferenceRepository: PollenPreferenceRepository
) : SetPollenAllergenTask {

    override suspend fun invoke(
        pollenId: String,
        isAllergen: Boolean
    ): Either<Failure, Unit> =
        pollenPreferenceRepository.setAllergen(
            pollenId = pollenId,
            isAllergen = isAllergen
        )
}