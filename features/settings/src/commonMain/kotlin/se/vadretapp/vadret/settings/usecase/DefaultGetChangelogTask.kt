package se.vadretapp.vadret.settings.usecase

import io.github.sphrak.either.Either
import se.vadretapp.vadret.changelog.ChangelogRepository
import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.common.Failure

internal class DefaultGetChangelogTask(
    private val changelogRepository: ChangelogRepository
) : GetChangelogTask {

    override suspend fun invoke(): Either<Failure, Changelog> = changelogRepository.getChangelog()
}