package fi.kroon.vadret.pushalert.usecase

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pollen.model.PollenType
import io.github.sphrak.either.Either

internal interface GetPollenTypesTask {
    suspend operator fun invoke(): Either<Failure, List<PollenType>>
}