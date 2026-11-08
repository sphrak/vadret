package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import io.github.sphrak.either.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastpreference.ForecastPreferenceRepository
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences
import se.vadretapp.vadret.settings.model.Labs

internal class DefaultObserveLabsTask(
    private val forecastPreferenceRepository: ForecastPreferenceRepository
) : ObserveLabsTask {

    override fun invoke(): Flow<Either<Failure, Labs>> =
        forecastPreferenceRepository
            .observeForecastPreferences()
            .map { forecastPreferences: Either<Failure, ForecastPreferences> ->
                forecastPreferences.map { preferences: ForecastPreferences ->
                    Labs(
                        isHourlyForecastEnabled = preferences.isHourlyForecastEnabled,
                        isAuroraEnabled = preferences.isAuroraEnabled
                    )
                }
            }
}