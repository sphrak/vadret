package se.vadretapp.vadret.setting.extension

import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormatChoice
import se.vadretapp.vadret.setting.model.Theme

internal fun String?.toTheme(): Theme = Theme.entries.firstOrNull { theme: Theme -> theme.name == this } ?: Theme.SYSTEM

internal fun String?.toAppLanguage(): AppLanguage = AppLanguage.entries.firstOrNull { language: AppLanguage -> language.name == this } ?: AppLanguage.SYSTEM

internal fun String?.toHourFormatChoice(): HourFormatChoice =
    HourFormatChoice.entries.firstOrNull { hourFormatChoice: HourFormatChoice -> hourFormatChoice.name == this } ?: HourFormatChoice.SYSTEM