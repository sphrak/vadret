package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.common.Failure

internal interface GetChangelogTask {
    suspend operator fun invoke(): Either<Failure, Changelog>
}