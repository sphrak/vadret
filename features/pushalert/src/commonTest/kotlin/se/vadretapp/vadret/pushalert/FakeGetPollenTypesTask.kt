package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import kotlinx.coroutines.CompletableDeferred
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pushalert.usecase.GetPollenTypesTask

internal class FakeGetPollenTypesTask(
    val pollenTypes: CompletableDeferred<Either<Failure, List<PollenType>>>
) : GetPollenTypesTask {
    override suspend fun invoke(): Either<Failure, List<PollenType>> = pollenTypes.await()
}