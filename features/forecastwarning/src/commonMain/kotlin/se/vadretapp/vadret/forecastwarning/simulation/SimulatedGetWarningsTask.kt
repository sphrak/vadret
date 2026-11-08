package se.vadretapp.vadret.forecastwarning.simulation

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.warning.usecase.GetWarningsTask
import kotlin.time.Clock

internal class SimulatedGetWarningsTask(
    private val clock: Clock
) : GetWarningsTask {

    override suspend fun invoke(): Either<Failure, List<AggregatedFeed>> = simulatedWarnings(now = clock.now()).asRight()
}