package se.vadretapp.vadret.pollenpreference

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.pollenpreference.extension.toFailure
import se.vadretapp.vadret.pollenpreference.extension.toPollenPreferences
import se.vadretapp.vadret.pollenpreference.local.PollenPreferenceLocalDataSource
import se.vadretapp.vadret.pollenpreference.local.PollenPreferencesEntity
import se.vadretapp.vadret.pollenpreference.model.PollenPreferences

internal class DefaultPollenPreferenceRepository(
    private val pollenPreferenceLocalDataSource: PollenPreferenceLocalDataSource
) : PollenPreferenceRepository {

    override suspend fun getPollenPreferences(): Either<Failure, PollenPreferences> =
        either { pollenPreferenceLocalDataSource.getPollenPreferences().toPollenPreferences() }
            .catch { exception: Exception -> exception.toFailure() }

    override fun observePollenPreferences(): Flow<Either<Failure, PollenPreferences>> =
        pollenPreferenceLocalDataSource
            .observePollenPreferences()
            .map<PollenPreferencesEntity, Either<Failure, PollenPreferences>> { pollenPreferencesEntity: PollenPreferencesEntity ->
                pollenPreferencesEntity.toPollenPreferences().asRight()
            }.catch { throwable: Throwable ->
                if (throwable is Exception) {
                    emit(throwable.toFailure().asLeft())
                } else {
                    throw throwable
                }
            }

    override suspend fun setPollenEnabled(isEnabled: Boolean): Either<Failure, Unit> =
        either { pollenPreferenceLocalDataSource.setPollenEnabled(value = isEnabled) }
            .catch { exception: Exception -> exception.toFailure() }

    override suspend fun setAllergen(
        pollenId: String,
        isAllergen: Boolean
    ): Either<Failure, Unit> =
        either {
            pollenPreferenceLocalDataSource.setAllergen(
                pollenId = pollenId,
                isAllergen = isAllergen
            )
        }.catch { exception: Exception -> exception.toFailure() }

    override suspend fun setHibernationNoticeShown(isShown: Boolean): Either<Failure, Unit> =
        either { pollenPreferenceLocalDataSource.setHibernationNoticeShown(value = isShown) }
            .catch { exception: Exception -> exception.toFailure() }

    override suspend fun setEndOfSeasonSimulated(isSimulated: Boolean): Either<Failure, Unit> =
        either { pollenPreferenceLocalDataSource.setEndOfSeasonSimulated(value = isSimulated) }
            .catch { exception: Exception -> exception.toFailure() }
}