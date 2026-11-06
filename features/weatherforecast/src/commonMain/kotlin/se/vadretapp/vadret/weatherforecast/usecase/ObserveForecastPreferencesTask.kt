package se.vadretapp.vadret.weatherforecast.usecase

import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences

internal interface ObserveForecastPreferencesTask {

    operator fun invoke(): Flow<Either<Failure, ForecastPreferences>>
}