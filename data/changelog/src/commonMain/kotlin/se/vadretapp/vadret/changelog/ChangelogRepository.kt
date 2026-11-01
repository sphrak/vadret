package se.vadretapp.vadret.changelog

import io.github.sphrak.either.Either
import kotlinx.coroutines.CoroutineDispatcher
import se.vadretapp.vadret.changelog.local.DefaultChangelogLocalDataSource
import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.common.Failure

interface ChangelogRepository {
    suspend fun getChangelog(): Either<Failure, Changelog>
}

fun ChangelogRepository(
    readChangelog: suspend () -> String,
    coroutineDispatcher: CoroutineDispatcher
): ChangelogRepository =
    DefaultChangelogRepository(
        changelogLocalDataSource = DefaultChangelogLocalDataSource(
            readChangelog = readChangelog,
            coroutineDispatcher = coroutineDispatcher
        )
    )