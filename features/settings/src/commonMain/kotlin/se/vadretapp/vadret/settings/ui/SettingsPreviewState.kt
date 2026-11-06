package se.vadretapp.vadret.settings.ui

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.model.FavoriteChoice
import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.library.model.Library
import se.vadretapp.vadret.settings.AboutState
import se.vadretapp.vadret.settings.DeveloperState
import se.vadretapp.vadret.settings.LicensesState
import se.vadretapp.vadret.settings.PlacesState
import se.vadretapp.vadret.settings.PollenSettingsState
import se.vadretapp.vadret.settings.Settings
import se.vadretapp.vadret.settings.SettingsState
import se.vadretapp.vadret.settings.aboutRows
import se.vadretapp.vadret.settings.extension.toLibraryGroups
import se.vadretapp.vadret.settings.model.PollenAllergenRow
import se.vadretapp.vadret.settings.model.PollenGroup
import se.vadretapp.vadret.settings.model.PollenGroupRows
import se.vadretapp.vadret.settings.model.RowSummary
import se.vadretapp.vadret.theme.model.Theme

private const val PREVIEW_APP_VERSION = "3.1.0"
private const val PREVIEW_PLACE_NAME = "Stockholm"

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

internal fun settingsPreviewState(theme: Theme): SettingsState =
    SettingsState(
        settings = Settings(
            theme = theme,
            language = AppLanguage.SYSTEM,
            isHourlyForecastEnabled = false,
            isExpandOnceEnabled = true,
            isPollenEnabled = true,
            allergenPollenIds = emptySet(),
            isEndOfSeasonSimulated = false
        ),
        placesSummary = RowSummary.Plain(text = PREVIEW_PLACE_NAME),
        isDeveloperRowVisible = true
    )

internal fun licensesPreviewState(): LicensesState = LicensesState(libraryGroups = previewLibraries.toLibraryGroups())

internal fun aboutPreviewState(): AboutState = AboutState(aboutRows = aboutRows(appVersion = PREVIEW_APP_VERSION))

private val previewFavorites: List<Favorite> = listOf(
    Favorite(
        favoriteId = "preview-stockholm",
        locality = "Stockholm",
        municipality = "Stockholm",
        county = "Stockholms län",
        latitude = 59.3293,
        longitude = 18.0686
    ),
    Favorite(
        favoriteId = "preview-rumshamn",
        locality = "Rumshamn",
        municipality = "Gotland",
        county = "Gotlands län",
        latitude = 57.6203,
        longitude = 18.7836
    )
)

internal fun placesPreviewState(): PlacesState =
    PlacesState(
        favoriteChoices = previewFavorites
            .mapIndexed { index: Int, favorite: Favorite ->
                FavoriteChoice(
                    favorite = favorite,
                    isSelected = index == 0,
                    isFollowed = true
                )
            }.toImmutableList()
    )

internal fun pollenSettingsPreviewState(): PollenSettingsState =
    PollenSettingsState(
        isLoading = false,
        isPollenEnabled = true,
        isOffSeason = true,
        pollenGroupRows = persistentListOf(
            PollenGroupRows(
                pollenGroup = PollenGroup.TREES,
                pollenAllergenRows = persistentListOf(
                    PollenAllergenRow(pollenId = "hazel", name = "Hassel", isAllergen = false),
                    PollenAllergenRow(pollenId = "birch", name = "Björk", isAllergen = true)
                )
            ),
            PollenGroupRows(
                pollenGroup = PollenGroup.GRASSES_AND_HERBS,
                pollenAllergenRows = persistentListOf(PollenAllergenRow(pollenId = "grass", name = "Gräs", isAllergen = true))
            )
        )
    )

internal fun developerPreviewState(theme: Theme): DeveloperState = DeveloperState(settings = settingsPreviewState(theme = theme).settings)