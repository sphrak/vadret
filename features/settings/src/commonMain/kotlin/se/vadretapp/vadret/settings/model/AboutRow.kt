package se.vadretapp.vadret.settings.model

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

internal data class AboutRow(
    val iconResource: DrawableResource,
    val titleResource: StringResource,
    val hint: AboutRowHint?,
    val action: AboutRowAction,
    val trailingIconResource: DrawableResource?
)