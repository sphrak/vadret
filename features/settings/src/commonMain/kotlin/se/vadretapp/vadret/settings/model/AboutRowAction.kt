package se.vadretapp.vadret.settings.model

import org.jetbrains.compose.resources.StringResource

internal sealed interface AboutRowAction {
    data class OpenUrl(
        val urlResource: StringResource
    ) : AboutRowAction

    data object ShowChangelog : AboutRowAction

    data object ShowLicenses : AboutRowAction

    data object CountVersionTap : AboutRowAction
}