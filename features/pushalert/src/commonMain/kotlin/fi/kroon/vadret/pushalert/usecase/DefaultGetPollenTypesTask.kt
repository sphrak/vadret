package fi.kroon.vadret.pushalert.usecase

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pollen.PollenRepository
import fi.kroon.vadret.pollen.model.PollenType
import io.github.sphrak.either.Either

internal class DefaultGetPollenTypesTask(
    private val pollenRepository: PollenRepository
) : GetPollenTypesTask {
    override suspend fun invoke(): Either<Failure, List<PollenType>> = pollenRepository.getPollenTypes()
}