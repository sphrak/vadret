package se.vadretapp.vadret.settings.ui

import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.library.model.Library
import se.vadretapp.vadret.location.model.StoredLocation
import se.vadretapp.vadret.settings.Settings
import se.vadretapp.vadret.settings.SettingsState
import se.vadretapp.vadret.settings.aboutRows
import se.vadretapp.vadret.settings.extension.toLibraryGroups
import se.vadretapp.vadret.settings.extension.toLocationRows
import se.vadretapp.vadret.theme.model.Theme

private const val PREVIEW_APP_VERSION = "3.1.0"

private val previewLocation = StoredLocation(
    locality = "Stockholm",
    municipality = "Stockholm",
    county = "Stockholms län",
    latitude = 59.3293,
    longitude = 18.0686
)

private val previewLibraries: List<Library> = listOf(
    Library(
        author = "JetBrains",
        title = "Kotlin",
        description = "Statically typed programming language",
        projectUrl = "https://kotlinlang.org",
        sourceUrl = "https://github.com/JetBrains/kotlin",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        license = "Apache 2.0"
    ),
    Library(
        author = "Google",
        title = "Jetpack Compose",
        description = "Modern toolkit for building native Android UI",
        projectUrl = "https://developer.android.com/compose",
        sourceUrl = "https://android.googlesource.com/platform/frameworks/support",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        license = "Apache 2.0"
    )
)

private const val PREVIEW_CHANGELOG = """# Changelog

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [3.1.0] -- 2026-10-26

### Added

- New 'Coming days' view

### Changed

- Interpolate between wind and precipitation frames
"""

internal fun changelogPreview(): Changelog = Changelog(markdown = PREVIEW_CHANGELOG)

internal fun settingsPreviewState(theme: Theme): SettingsState {
    val settings = Settings(
        isAutomaticLocation = false,
        theme = theme,
        language = AppLanguage.SYSTEM,
        isHourlyForecastEnabled = false,
        isExpandOnceEnabled = true,
        storedLocation = previewLocation
    )
    return SettingsState(
        settings = settings,
        locationRows = settings.toLocationRows(),
        libraryGroups = previewLibraries.toLibraryGroups(),
        aboutRows = aboutRows(appVersion = PREVIEW_APP_VERSION)
    )
}