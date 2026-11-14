package se.vadretapp.vadret.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.LanguageEnvironment
import se.vadretapp.vadret.ui.VadretTheme

@Composable
internal fun AppEnvironment(
    mainViewModel: MainViewModel,
    languageTagOverride: String?,
    systemBarsEffect: @Composable (theme: Theme) -> Unit,
    content: @Composable () -> Unit
) {
    val theme: Theme? by mainViewModel.theme.collectAsStateWithLifecycle()
    val language: AppLanguage? by mainViewModel.language.collectAsStateWithLifecycle()
    val hourFormat: HourFormat? by mainViewModel.hourFormat.collectAsStateWithLifecycle()
    val currentTheme: Theme = theme ?: return
    val currentLanguage: AppLanguage = language ?: return
    val currentHourFormat: HourFormat = hourFormat ?: return
    systemBarsEffect(currentTheme)
    LanguageEnvironment(
        languageTag = languageTagOverride ?: currentLanguage.languageTag,
        hourFormat = currentHourFormat
    ) {
        VadretTheme(theme = currentTheme) {
            content()
        }
    }
}