package se.vadretapp.vadret.theme.extension

import se.vadretapp.vadret.theme.model.Theme

internal fun String?.toTheme(): Theme = Theme.entries.firstOrNull { theme: Theme -> theme.name == this } ?: Theme.SYSTEM