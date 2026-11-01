package se.vadretapp.vadret.forecastpreference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.forecastpreference.local.DefaultForecastPreferenceLocalDataSource
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences

interface ForecastPreferenceRepository {
    suspend fun getForecastPreferences(): Either<Failure, ForecastPreferences>
    fun observeForecastPreferences(): Flow<Either<Failure, ForecastPreferences>>
    suspend fun setHourlyForecastEnabled(isEnabled: Boolean): Either<Failure, Unit>
    suspend fun setExpandOnceEnabled(isEnabled: Boolean): Either<Failure, Unit>
}

fun ForecastPreferenceRepository(dataStore: DataStore<Preferences>): ForecastPreferenceRepository =
    DefaultForecastPreferenceRepository(
        forecastPreferenceLocalDataSource = DefaultForecastPreferenceLocalDataSource(dataStore = dataStore)
    )