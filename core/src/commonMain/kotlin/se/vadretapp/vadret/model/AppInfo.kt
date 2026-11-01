package se.vadretapp.vadret.model

data class AppInfo(
    val applicationId: String,
    val versionName: String,
    val isDebug: Boolean,
    val isDeveloperBuild: Boolean
)