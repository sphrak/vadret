package se.vadretapp.vadret.settings

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.settings.model.AboutRow
import se.vadretapp.vadret.settings.model.AboutRowAction
import se.vadretapp.vadret.settings.model.RowSummary
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.app_github_source_url
import se.vadretapp.vadret.ui.resources.app_license_url
import se.vadretapp.vadret.ui.resources.app_privacy_url
import se.vadretapp.vadret.ui.resources.changelog_row_title
import se.vadretapp.vadret.ui.resources.ic_chevron_right
import se.vadretapp.vadret.ui.resources.ic_code
import se.vadretapp.vadret.ui.resources.ic_copyright
import se.vadretapp.vadret.ui.resources.ic_description
import se.vadretapp.vadret.ui.resources.ic_history
import se.vadretapp.vadret.ui.resources.ic_info
import se.vadretapp.vadret.ui.resources.ic_open_in_new
import se.vadretapp.vadret.ui.resources.ic_shield
import se.vadretapp.vadret.ui.resources.license_row_hint
import se.vadretapp.vadret.ui.resources.license_row_title
import se.vadretapp.vadret.ui.resources.licenses_title
import se.vadretapp.vadret.ui.resources.privacy_policy_row_title
import se.vadretapp.vadret.ui.resources.source_code_row_title
import se.vadretapp.vadret.ui.resources.version_row_title

internal fun aboutRows(appVersion: String): ImmutableList<AboutRow> =
    persistentListOf(
        AboutRow(
            iconResource = Res.drawable.ic_info,
            titleResource = Res.string.version_row_title,
            summary = RowSummary.Plain(text = appVersion),
            action = AboutRowAction.CountVersionTap,
            trailingIconResource = null
        ),
        AboutRow(
            iconResource = Res.drawable.ic_copyright,
            titleResource = Res.string.license_row_title,
            summary = RowSummary.Resource(resource = Res.string.license_row_hint),
            action = AboutRowAction.OpenUrl(urlResource = Res.string.app_license_url),
            trailingIconResource = Res.drawable.ic_open_in_new
        ),
        AboutRow(
            iconResource = Res.drawable.ic_shield,
            titleResource = Res.string.privacy_policy_row_title,
            summary = null,
            action = AboutRowAction.OpenUrl(urlResource = Res.string.app_privacy_url),
            trailingIconResource = Res.drawable.ic_open_in_new
        ),
        AboutRow(
            iconResource = Res.drawable.ic_history,
            titleResource = Res.string.changelog_row_title,
            summary = null,
            action = AboutRowAction.ShowChangelog,
            trailingIconResource = Res.drawable.ic_chevron_right
        ),
        AboutRow(
            iconResource = Res.drawable.ic_code,
            titleResource = Res.string.source_code_row_title,
            summary = null,
            action = AboutRowAction.OpenUrl(urlResource = Res.string.app_github_source_url),
            trailingIconResource = Res.drawable.ic_open_in_new
        ),
        AboutRow(
            iconResource = Res.drawable.ic_description,
            titleResource = Res.string.licenses_title,
            summary = null,
            action = AboutRowAction.ShowLicenses,
            trailingIconResource = Res.drawable.ic_chevron_right
        )
    )

internal data class AboutState(
    val aboutRows: ImmutableList<AboutRow> = persistentListOf(),
    val changelog: Changelog? = null,
    val isChangelogVisible: Boolean = false,
    val toastResource: StringResource? = null
)