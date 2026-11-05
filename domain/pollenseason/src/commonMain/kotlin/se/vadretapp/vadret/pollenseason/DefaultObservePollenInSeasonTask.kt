package se.vadretapp.vadret.pollenseason

import io.github.sphrak.either.Either
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollen.PollenRepository
import se.vadretapp.vadret.pollen.model.PollenForecast

internal class DefaultObservePollenInSeasonTask(
    private val pollenRepository: PollenRepository
) : ObservePollenInSeasonTask {

    override fun invoke(): Flow<Either<Failure, Boolean>> =
        flow {
            emit(
                value = pollenRepository
                    .getCurrentForecasts()
                    .map { pollenForecasts: List<PollenForecast> ->
                        pollenForecasts.any { pollenForecast: PollenForecast -> !pollenForecast.isEndOfSeason && pollenForecast.dailyLevels.isNotEmpty() }
                    }
            )
        }
}