package se.vadretapp.vadret.settings.extension

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.amoled_theme
import se.vadretapp.vadret.ui.resources.dark_theme
import se.vadretapp.vadret.ui.resources.light_theme
import se.vadretapp.vadret.ui.resources.system_theme

internal fun Theme.labelResource(): StringResource =
    when (this) {
        Theme.LIGHT -> Res.string.light_theme
        Theme.DARK -> Res.string.dark_theme
        Theme.AMOLED -> Res.string.amoled_theme
        Theme.SYSTEM -> Res.string.system_theme
    }