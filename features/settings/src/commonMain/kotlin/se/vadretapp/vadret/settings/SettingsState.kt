package se.vadretapp.vadret.settings

import se.vadretapp.vadret.settings.model.RowSummary

internal data class SettingsState(
    val settings: Settings? = null,
    val placesSummary: RowSummary? = null,
    val isDeveloperRowVisible: Boolean = false,
    val isThemeDialogVisible: Boolean = false,
    val isLanguageDialogVisible: Boolean = false
)