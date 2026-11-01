package se.vadretapp.vadret.settings.extension

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.amoled_key
import se.vadretapp.vadret.core.resources.dark_key
import se.vadretapp.vadret.core.resources.light_key
import se.vadretapp.vadret.theme.model.Theme

internal fun Theme.labelResource(): StringResource =
    when (this) {
        Theme.LIGHT -> Res.string.light_key
        Theme.DARK -> Res.string.dark_key
        Theme.AMOLED -> Res.string.amoled_key
    }