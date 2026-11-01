package se.vadretapp.vadret.changelog.local

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal class DefaultChangelogLocalDataSource(
    private val readChangelog: suspend () -> String,
    private val coroutineDispatcher: CoroutineDispatcher
) : ChangelogLocalDataSource {

    override suspend fun getChangelog(): String =
        withContext(context = coroutineDispatcher) {
            readChangelog()
        }
}