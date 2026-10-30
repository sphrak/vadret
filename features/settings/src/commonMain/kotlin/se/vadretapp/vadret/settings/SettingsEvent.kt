package se.vadretapp.vadret.settings

import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.library.model.Library
import se.vadretapp.vadret.theme.model.Theme

internal sealed interface SettingsEvent {
    data object OnStart : SettingsEvent
    data class OnLocationModeToggled(val isAutomaticLocation: Boolean) : SettingsEvent
    data class OnHourlyForecastToggled(val isHourlyForecastEnabled: Boolean) : SettingsEvent
    data class OnExpandOnceToggled(val isExpandOnceEnabled: Boolean) : SettingsEvent
    data object OnThemeRowClicked : SettingsEvent
    data object OnThemeDialogDismissed : SettingsEvent
    data class OnThemeSelected(val theme: Theme) : SettingsEvent
    data object OnLanguageRowClicked : SettingsEvent
    data object OnLanguageDialogDismissed : SettingsEvent
    data class OnLanguageSelected(val language: AppLanguage) : SettingsEvent
    data class OnLibraryClicked(val library: Library) : SettingsEvent
    data object OnLibraryDismissed : SettingsEvent
    data object OnChangelogClicked : SettingsEvent
    data object OnChangelogDismissed : SettingsEvent
    data class OnPollenToggled(val isPollenEnabled: Boolean) : SettingsEvent
    data object OnPollenAllergensRowClicked : SettingsEvent
    data object OnPollenAllergensDialogDismissed : SettingsEvent
    data class OnPollenAllergenToggled(
        val pollenId: String,
        val isAllergen: Boolean
    ) : SettingsEvent
    data class OnEndOfSeasonSimulationToggled(val isEndOfSeasonSimulated: Boolean) : SettingsEvent
}