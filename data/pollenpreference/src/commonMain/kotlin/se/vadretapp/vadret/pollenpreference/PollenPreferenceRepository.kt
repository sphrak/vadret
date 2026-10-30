package se.vadretapp.vadret.pollenpreference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.sphrak.either.Either
import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollenpreference.local.DefaultPollenPreferenceLocalDataSource
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences

interface PollenPreferenceRepository {

    suspend fun getPollenPreferences(): Either<Failure, PollenPreferences>

    fun observePollenPreferences(): Flow<Either<Failure, PollenPreferences>>

    suspend fun setPollenEnabled(isEnabled: Boolean): Either<Failure, Unit>

    suspend fun setAllergen(
        pollenId: String,
        isAllergen: Boolean
    ): Either<Failure, Unit>

    suspend fun setHibernationNoticeShown(isShown: Boolean): Either<Failure, Unit>

    suspend fun setEndOfSeasonSimulated(isSimulated: Boolean): Either<Failure, Unit>
}

fun PollenPreferenceRepository(dataStore: DataStore<Preferences>): PollenPreferenceRepository =
    DefaultPollenPreferenceRepository(
        pollenPreferenceLocalDataSource = DefaultPollenPreferenceLocalDataSource(dataStore = dataStore)
    )