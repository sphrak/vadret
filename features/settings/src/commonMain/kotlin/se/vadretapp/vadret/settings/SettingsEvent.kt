package se.vadretapp.vadret.settings

import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormatChoice
import se.vadretapp.vadret.setting.model.Theme

internal sealed interface SettingsEvent {
    data object OnStart : SettingsEvent
    data object OnThemeRowClicked : SettingsEvent
    data object OnThemeDialogDismissed : SettingsEvent
    data class OnThemeSelected(val theme: Theme) : SettingsEvent
    data object OnLanguageRowClicked : SettingsEvent
    data object OnLanguageDialogDismissed : SettingsEvent
    data class OnLanguageSelected(val language: AppLanguage) : SettingsEvent
    data object OnHourFormatRowClicked : SettingsEvent
    data object OnHourFormatDialogDismissed : SettingsEvent
    data class OnHourFormatSelected(val hourFormatChoice: HourFormatChoice) : SettingsEvent
}