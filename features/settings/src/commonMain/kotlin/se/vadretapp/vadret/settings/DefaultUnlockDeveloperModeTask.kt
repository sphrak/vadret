package se.vadretapp.vadret.settings

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.developermode.DeveloperModeRepository

internal class DefaultUnlockDeveloperModeTask(
    private val developerModeRepository: DeveloperModeRepository
) : UnlockDeveloperModeTask {

    override suspend fun invoke(): Either<Failure, Unit> = developerModeRepository.unlockDeveloperMode()
}