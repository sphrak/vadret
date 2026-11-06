package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pushalert.usecase.ObservePollenTypesTask

internal class FakeObservePollenTypesTask(
    val pollenTypes: CompletableDeferred<Either<Failure, List<PollenType>>>
) : ObservePollenTypesTask {
    override fun invoke(): Flow<Either<Failure, List<PollenType>>> = flow { emit(value = pollenTypes.await()) }
}