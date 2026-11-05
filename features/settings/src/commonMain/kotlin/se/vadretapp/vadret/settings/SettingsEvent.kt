package se.vadretapp.vadret.settings

import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.theme.model.Theme

internal sealed interface SettingsEvent {
    data object OnStart : SettingsEvent
    data class OnHourlyForecastToggled(val isHourlyForecastEnabled: Boolean) : SettingsEvent
    data object OnThemeRowClicked : SettingsEvent
    data object OnThemeDialogDismissed : SettingsEvent
    data class OnThemeSelected(val theme: Theme) : SettingsEvent
    data object OnLanguageRowClicked : SettingsEvent
    data object OnLanguageDialogDismissed : SettingsEvent
    data class OnLanguageSelected(val language: AppLanguage) : SettingsEvent
}