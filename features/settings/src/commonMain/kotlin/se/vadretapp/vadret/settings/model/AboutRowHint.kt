package se.vadretapp.vadret.settings.model

import org.jetbrains.compose.resources.StringResource

internal sealed interface AboutRowHint {

    data class Resource(val resource: StringResource) : AboutRowHint

    data class Plain(val text: String) : AboutRowHint
}