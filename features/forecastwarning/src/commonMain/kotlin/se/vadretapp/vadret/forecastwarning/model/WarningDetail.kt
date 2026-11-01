package se.vadretapp.vadret.forecastwarning.model

import org.jetbrains.compose.resources.StringResource

internal sealed interface WarningDetail {

    val text: String

    data class Labelled(
        val labelResource: StringResource,
        override val text: String
    ) : WarningDetail

    data class Titled(
        val title: String,
        override val text: String
    ) : WarningDetail

    data class Paragraph(
        override val text: String
    ) : WarningDetail
}