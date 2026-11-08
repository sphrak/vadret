package se.vadretapp.vadret.forecastwarning.simulation

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import se.vadretapp.vadret.aggregatedfeed.model.Aurora
import se.vadretapp.vadret.aurora.model.AuroraHour
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.warning.usecase.GetAuroraHoursTask
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private const val HOURS_BEFORE_PEAK = 3
private const val HOURS_AFTER_PEAK = 3
private const val SCORE_LOST_PER_HOUR_FROM_PEAK = 12
private const val LOWEST_SCORE = 0

// The night of a simulated aurora rises to its score at its peak and falls away on both sides of it.
internal class SimulatedGetAuroraHoursTask : GetAuroraHoursTask {

    override suspend fun invoke(aurora: Aurora): Either<Failure, List<AuroraHour>> {
        val peakAt: Instant = aurora.peakAt ?: return emptyList<AuroraHour>().asRight()
        return (-HOURS_BEFORE_PEAK..HOURS_AFTER_PEAK).map { hoursFromPeak: Int ->
            AuroraHour(
                time = peakAt + hoursFromPeak.hours,
                score = (aurora.score - hoursFromPeak.absoluteValue * SCORE_LOST_PER_HOUR_FROM_PEAK).coerceAtLeast(minimumValue = LOWEST_SCORE)
            )
        }.asRight()
    }
}