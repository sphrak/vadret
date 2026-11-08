package se.vadretapp.vadret.pushalert.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastpreference.ForecastPreferenceRepository
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences

internal class DefaultObserveAuroraEnabledTask(
    private val forecastPreferenceRepository: ForecastPreferenceRepository
) : ObserveAuroraEnabledTask {
    override fun invoke(): Flow<Either<Failure, Boolean>> =
        forecastPreferenceRepository
            .observeForecastPreferences()
            .map { forecastPreferences: Either<Failure, ForecastPreferences> ->
                forecastPreferences.map { preferences: ForecastPreferences -> preferences.isAuroraEnabled }
            }
}