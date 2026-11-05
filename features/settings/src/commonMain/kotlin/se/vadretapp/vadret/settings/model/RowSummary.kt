package se.vadretapp.vadret.settings.model

import org.jetbrains.compose.resources.StringResource

internal sealed interface RowSummary {

    data class Resource(val resource: StringResource) : RowSummary

    data class Plain(val text: String) : RowSummary
}