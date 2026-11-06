package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollen.model.PollenType

internal interface ObservePollenTypesTask {
    operator fun invoke(): Flow<Either<Failure, List<PollenType>>>
}