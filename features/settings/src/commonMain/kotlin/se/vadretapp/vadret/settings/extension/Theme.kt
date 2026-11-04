package se.vadretapp.vadret.settings.extension

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.amoled_theme
import se.vadretapp.vadret.core.resources.dark_theme
import se.vadretapp.vadret.core.resources.light_theme
import se.vadretapp.vadret.core.resources.system_theme
import se.vadretapp.vadret.theme.model.Theme

internal fun Theme.labelResource(): StringResource =
    when (this) {
        Theme.LIGHT -> Res.string.light_theme
        Theme.DARK -> Res.string.dark_theme
        Theme.AMOLED -> Res.string.amoled_theme
        Theme.SYSTEM -> Res.string.system_theme
    }