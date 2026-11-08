package se.vadretapp.vadret.warning.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.aggregatedfeed.model.Aurora
import se.vadretapp.vadret.aurora.model.AuroraHour
import se.vadretapp.vadret.common.Failure

interface GetAuroraHoursTask {

    suspend operator fun invoke(aurora: Aurora): Either<Failure, List<AuroraHour>>
}