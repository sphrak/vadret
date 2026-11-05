package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.developermode.DeveloperModeRepository
import se.vadretapp.vadret.di.model.AppInfo

internal class DefaultObserveDeveloperModeTask(
    private val developerModeRepository: DeveloperModeRepository,
    private val appInfo: AppInfo
) : ObserveDeveloperModeTask {

    // A developer build has the developer settings from the start, other builds once they are unlocked.
    override fun invoke(): Flow<Either<Failure, Boolean>> =
        developerModeRepository
            .observeIsDeveloperModeUnlocked()
            .map { isUnlocked: Either<Failure, Boolean> ->
                if (appInfo.isDeveloperBuild) {
                    true.asRight()
                } else {
                    isUnlocked
                }
            }
}