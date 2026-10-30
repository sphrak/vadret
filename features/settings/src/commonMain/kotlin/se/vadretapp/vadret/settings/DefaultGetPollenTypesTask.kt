package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import io.github.sphrak.either.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollen.PollenRepository
import se.vadretapp.vadret.pollen.model.PollenType

internal class DefaultGetPollenTypesTask(
    private val pollenRepository: PollenRepository
) : GetPollenTypesTask {

    override suspend fun invoke(): Either<Failure, List<PollenType>> =
        pollenRepository
            .getPollenTypes()
            .map { pollenTypes: List<PollenType> -> pollenTypes.filter { pollenType: PollenType -> pollenType.hasForecasts } }
}