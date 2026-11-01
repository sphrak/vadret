package fi.kroon.vadret.pushalert

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pollen.model.PollenType
import fi.kroon.vadret.pushalert.usecase.GetPollenTypesTask
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight

internal class FakeGetPollenTypesTask(
    private val pollenTypes: List<PollenType>
) : GetPollenTypesTask {
    override suspend fun invoke(): Either<Failure, List<PollenType>> = pollenTypes.asRight()
}