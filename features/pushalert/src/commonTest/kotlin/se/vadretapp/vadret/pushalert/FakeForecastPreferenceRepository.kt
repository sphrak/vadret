package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastpreference.ForecastPreferenceRepository
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences

internal class FakeForecastPreferenceRepository(
    isAuroraEnabled: Boolean = true
) : ForecastPreferenceRepository {

    val forecastPreferences: MutableStateFlow<Either<Failure, ForecastPreferences>> = MutableStateFlow(
        value = ForecastPreferences(
            isHourlyForecastEnabled = false,
            isExpandOnceEnabled = true,
            isAuroraEnabled = isAuroraEnabled
        ).asRight()
    )

    override suspend fun getForecastPreferences(): Either<Failure, ForecastPreferences> = forecastPreferences.value

    override fun observeForecastPreferences(): Flow<Either<Failure, ForecastPreferences>> = forecastPreferences

    override suspend fun setHourlyForecastEnabled(isEnabled: Boolean): Either<Failure, Unit> = error("Push alerts never turn the hourly forecast on or off")

    override suspend fun setExpandOnceEnabled(isEnabled: Boolean): Either<Failure, Unit> = error("Push alerts never change how days expand")

    override suspend fun setAuroraEnabled(isEnabled: Boolean): Either<Failure, Unit> = error("Push alerts never turn the aurora lab on or off")
}