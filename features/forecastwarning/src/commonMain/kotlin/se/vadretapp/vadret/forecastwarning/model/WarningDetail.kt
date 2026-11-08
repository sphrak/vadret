package se.vadretapp.vadret.forecastwarning.model

import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.StringResource

internal sealed interface WarningDetail {

    data class Labelled(
        val labelResource: StringResource,
        val text: String
    ) : WarningDetail

    data class Titled(
        val title: String,
        val text: String
    ) : WarningDetail

    data class Paragraph(
        val text: String
    ) : WarningDetail

    data class Explained(
        val labelResource: StringResource,
        val textResource: StringResource
    ) : WarningDetail

    data class Hourly(
        val labelResource: StringResource,
        val auroraHourItems: ImmutableList<AuroraHourItem>
    ) : WarningDetail
}