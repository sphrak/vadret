package se.vadretapp.vadret.settings

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.app_github_source_url
import se.vadretapp.vadret.core.resources.app_license_url
import se.vadretapp.vadret.core.resources.app_privacy_url
import se.vadretapp.vadret.core.resources.app_release_page
import se.vadretapp.vadret.core.resources.changelog_row_title
import se.vadretapp.vadret.core.resources.ic_chevron_right
import se.vadretapp.vadret.core.resources.ic_code
import se.vadretapp.vadret.core.resources.ic_copyright
import se.vadretapp.vadret.core.resources.ic_history
import se.vadretapp.vadret.core.resources.ic_info
import se.vadretapp.vadret.core.resources.ic_open_in_new
import se.vadretapp.vadret.core.resources.ic_shield
import se.vadretapp.vadret.core.resources.license_row_hint
import se.vadretapp.vadret.core.resources.license_row_title
import se.vadretapp.vadret.core.resources.privacy_policy_row_title
import se.vadretapp.vadret.core.resources.source_code_row_title
import se.vadretapp.vadret.core.resources.version_row_title
import se.vadretapp.vadret.library.model.Library
import se.vadretapp.vadret.settings.model.AboutRow
import se.vadretapp.vadret.settings.model.AboutRowAction
import se.vadretapp.vadret.settings.model.AboutRowHint
import se.vadretapp.vadret.settings.model.LibraryGroup
import se.vadretapp.vadret.settings.model.LocationRow
import se.vadretapp.vadret.settings.ui.SettingsPage

internal fun aboutRows(appVersion: String): ImmutableList<AboutRow> =
    persistentListOf(
        AboutRow(
            iconResource = Res.drawable.ic_info,
            titleResource = Res.string.version_row_title,
            hint = AboutRowHint.Plain(text = appVersion),
            action = AboutRowAction.OpenUrl(urlResource = Res.string.app_release_page),
            trailingIconResource = null
        ),
        AboutRow(
            iconResource = Res.drawable.ic_copyright,
            titleResource = Res.string.license_row_title,
            hint = AboutRowHint.Resource(resource = Res.string.license_row_hint),
            action = AboutRowAction.OpenUrl(urlResource = Res.string.app_license_url),
            trailingIconResource = Res.drawable.ic_chevron_right
        ),
        AboutRow(
            iconResource = Res.drawable.ic_shield,
            titleResource = Res.string.privacy_policy_row_title,
            hint = null,
            action = AboutRowAction.OpenUrl(urlResource = Res.string.app_privacy_url),
            trailingIconResource = Res.drawable.ic_open_in_new
        ),
        AboutRow(
            iconResource = Res.drawable.ic_history,
            titleResource = Res.string.changelog_row_title,
            hint = null,
            action = AboutRowAction.ShowChangelog,
            trailingIconResource = Res.drawable.ic_chevron_right
        ),
        AboutRow(
            iconResource = Res.drawable.ic_code,
            titleResource = Res.string.source_code_row_title,
            hint = null,
            action = AboutRowAction.OpenUrl(urlResource = Res.string.app_github_source_url),
            trailingIconResource = Res.drawable.ic_open_in_new
        )
    )

internal fun settingsPages(isDeveloperBuild: Boolean): ImmutableList<SettingsPage> =
    SettingsPage.entries
        .filter { settingsPage: SettingsPage -> settingsPage != SettingsPage.DEVELOPER || isDeveloperBuild }
        .toImmutableList()

internal data class SettingsState(
    val pages: ImmutableList<SettingsPage> = settingsPages(isDeveloperBuild = false),
    val settings: Settings? = null,
    val locationRows: ImmutableList<LocationRow> = persistentListOf(),
    val libraryGroups: ImmutableList<LibraryGroup> = persistentListOf(),
    val selectedLibrary: Library? = null,
    val aboutRows: ImmutableList<AboutRow> = persistentListOf(),
    val isThemeDialogVisible: Boolean = false,
    val isLanguageDialogVisible: Boolean = false,
    val changelog: Changelog? = null,
    val isChangelogVisible: Boolean = false
)