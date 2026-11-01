package se.vadretapp.vadret.settings.ui

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.about_app_about_title
import se.vadretapp.vadret.core.resources.about_app_libraries_tab_title
import se.vadretapp.vadret.core.resources.developer_settings_title
import se.vadretapp.vadret.core.resources.settings

internal enum class SettingsPage(
    val titleResource: StringResource
) {
    PREFERENCES(titleResource = Res.string.settings),
    LIBRARIES(titleResource = Res.string.about_app_libraries_tab_title),
    ABOUT(titleResource = Res.string.about_app_about_title),
    DEVELOPER(titleResource = Res.string.developer_settings_title)
}