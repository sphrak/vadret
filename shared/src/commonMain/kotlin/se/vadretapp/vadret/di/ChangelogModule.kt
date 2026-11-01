package se.vadretapp.vadret.di

import org.koin.core.module.Module
import org.koin.dsl.module
import se.vadretapp.vadret.changelog.ChangelogRepository
import se.vadretapp.vadret.shared.resources.Res

private const val CHANGELOG_FILE = "files/changelog.md"

internal val changelogModule: Module = module {
    single<ChangelogRepository> {
        ChangelogRepository(
            readChangelog = { Res.readBytes(path = CHANGELOG_FILE).decodeToString() },
            coroutineDispatcher = get()
        )
    }
}