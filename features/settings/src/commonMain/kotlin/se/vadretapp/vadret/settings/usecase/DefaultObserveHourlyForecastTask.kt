package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastpreference.ForecastPreferenceRepository
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences

internal class DefaultObserveHourlyForecastTask(
    private val forecastPreferenceRepository: ForecastPreferenceRepository
) : ObserveHourlyForecastTask {

    override fun invoke(): Flow<Either<Failure, Boolean>> =
        forecastPreferenceRepository
            .observeForecastPreferences()
            .map { forecastPreferences: Either<Failure, ForecastPreferences> ->
                forecastPreferences.map { preferences: ForecastPreferences -> preferences.isHourlyForecastEnabled }
            }
}