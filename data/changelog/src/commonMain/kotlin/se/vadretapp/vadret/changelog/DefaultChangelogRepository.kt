package se.vadretapp.vadret.changelog

import io.github.sphrak.either.Either
import se.vadretapp.vadret.changelog.extension.toFailure
import se.vadretapp.vadret.changelog.local.ChangelogLocalDataSource
import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.common.catch
import se.vadretapp.vadret.common.either

internal class DefaultChangelogRepository(
    private val changelogLocalDataSource: ChangelogLocalDataSource
) : ChangelogRepository {

    override suspend fun getChangelog(): Either<Failure, Changelog> =
        either {
            Changelog(markdown = changelogLocalDataSource.getChangelog())
        }.catch { exception: Exception -> exception.toFailure() }
}