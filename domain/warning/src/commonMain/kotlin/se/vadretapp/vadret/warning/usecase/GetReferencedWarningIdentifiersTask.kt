package se.vadretapp.vadret.warning.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.common.Failure

interface GetReferencedWarningIdentifiersTask {

    suspend operator fun invoke(warningReference: WarningReference): Either<Failure, List<String>>
}