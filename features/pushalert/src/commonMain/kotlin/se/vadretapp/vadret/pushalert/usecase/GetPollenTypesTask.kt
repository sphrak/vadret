package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollen.model.PollenType

internal interface GetPollenTypesTask {
    suspend operator fun invoke(): Either<Failure, List<PollenType>>
}