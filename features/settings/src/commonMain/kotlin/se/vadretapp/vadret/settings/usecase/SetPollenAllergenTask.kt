package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure

internal interface SetPollenAllergenTask {
    suspend operator fun invoke(
        pollenId: String,
        isAllergen: Boolean
    ): Either<Failure, Unit>
}