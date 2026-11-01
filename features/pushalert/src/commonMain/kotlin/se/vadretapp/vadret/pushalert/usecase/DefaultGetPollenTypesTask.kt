package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollen.PollenRepository
import se.vadretapp.vadret.pollen.model.PollenType

internal class DefaultGetPollenTypesTask(
    private val pollenRepository: PollenRepository
) : GetPollenTypesTask {
    override suspend fun invoke(): Either<Failure, List<PollenType>> = pollenRepository.getPollenTypes()
}