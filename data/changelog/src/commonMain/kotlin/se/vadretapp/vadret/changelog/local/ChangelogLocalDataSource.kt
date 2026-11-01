package se.vadretapp.vadret.changelog.local

internal interface ChangelogLocalDataSource {
    suspend fun getChangelog(): String
}