package se.vadretapp.vadret.pollenseason

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.extension.successOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollenpreference.PollenPreferenceRepository
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences

internal class EndOfSeasonSimulatingObservePollenInSeasonTask(
    private val observePollenInSeasonTask: ObservePollenInSeasonTask,
    private val pollenPreferenceRepository: PollenPreferenceRepository
) : ObservePollenInSeasonTask {

    override fun invoke(): Flow<Either<Failure, Boolean>> =
        combine(
            flow = observePollenInSeasonTask(),
            flow2 = pollenPreferenceRepository.observePollenPreferences()
        ) { isInSeason: Either<Failure, Boolean>, pollenPreferences: Either<Failure, PollenPreferences> ->
            if (pollenPreferences.successOrNull?.isEndOfSeasonSimulated == true) {
                false.asRight()
            } else {
                isInSeason
            }
        }.distinctUntilChanged()
}