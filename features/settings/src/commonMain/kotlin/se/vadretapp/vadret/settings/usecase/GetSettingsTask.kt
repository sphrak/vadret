package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.settings.Settings

internal interface GetSettingsTask {
    suspend operator fun invoke(): Either<Failure, Settings>
}