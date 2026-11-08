package se.vadretapp.vadret.forecastpreference

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.forecastpreference.extension.toFailure
import se.vadretapp.vadret.forecastpreference.extension.toForecastPreferences
import se.vadretapp.vadret.forecastpreference.local.ForecastPreferenceLocalDataSource
import se.vadretapp.vadret.forecastpreference.local.ForecastPreferencesEntity
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences

internal class DefaultForecastPreferenceRepository(
    private val forecastPreferenceLocalDataSource: ForecastPreferenceLocalDataSource
) : ForecastPreferenceRepository {

    override suspend fun getForecastPreferences(): Either<Failure, ForecastPreferences> =
        either { forecastPreferenceLocalDataSource.getForecastPreferences().toForecastPreferences() }
            .catch { exception: Exception -> exception.toFailure() }

    override fun observeForecastPreferences(): Flow<Either<Failure, ForecastPreferences>> =
        forecastPreferenceLocalDataSource
            .observeForecastPreferences()
            .map<ForecastPreferencesEntity, Either<Failure, ForecastPreferences>> { forecastPreferencesEntity: ForecastPreferencesEntity ->
                forecastPreferencesEntity.toForecastPreferences().asRight()
            }.catch { throwable: Throwable ->
                if (throwable is Exception) {
                    emit(throwable.toFailure().asLeft())
                } else {
                    throw throwable
                }
            }

    override suspend fun setHourlyForecastEnabled(isEnabled: Boolean): Either<Failure, Unit> =
        either { forecastPreferenceLocalDataSource.setHourlyForecastEnabled(value = isEnabled) }
            .catch { exception: Exception -> exception.toFailure() }

    override suspend fun setExpandOnceEnabled(isEnabled: Boolean): Either<Failure, Unit> =
        either { forecastPreferenceLocalDataSource.setExpandOnceEnabled(value = isEnabled) }
            .catch { exception: Exception -> exception.toFailure() }

    override suspend fun setAuroraEnabled(isEnabled: Boolean): Either<Failure, Unit> =
        either { forecastPreferenceLocalDataSource.setAuroraEnabled(value = isEnabled) }
            .catch { exception: Exception -> exception.toFailure() }
}