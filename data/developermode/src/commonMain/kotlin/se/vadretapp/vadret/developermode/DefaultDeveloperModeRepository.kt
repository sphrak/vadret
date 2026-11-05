package se.vadretapp.vadret.developermode

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either
import se.vadretapp.vadret.developermode.extension.toFailure
import se.vadretapp.vadret.developermode.local.DeveloperModeLocalDataSource

internal class DefaultDeveloperModeRepository(
    private val developerModeLocalDataSource: DeveloperModeLocalDataSource
) : DeveloperModeRepository {

    override fun observeIsDeveloperModeUnlocked(): Flow<Either<Failure, Boolean>> =
        developerModeLocalDataSource
            .observeIsDeveloperModeUnlocked()
            .map<Boolean, Either<Failure, Boolean>> { isUnlocked: Boolean -> isUnlocked.asRight() }
            .catch { throwable: Throwable ->
                if (throwable is Exception) {
                    emit(throwable.toFailure().asLeft())
                } else {
                    throw throwable
                }
            }

    override suspend fun unlockDeveloperMode(): Either<Failure, Unit> =
        either { developerModeLocalDataSource.unlockDeveloperMode() }
            .catch { exception: Exception -> exception.toFailure() }
}