package se.vadretapp.vadret.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.main.navigation.TabNavigation
import se.vadretapp.vadret.main.navigation.TopLevelDestination
import se.vadretapp.vadret.main.navigation.WarningStoryKey
import se.vadretapp.vadret.main.navigation.open
import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.LanguageEnvironment
import se.vadretapp.vadret.ui.VadretTheme

@Composable
fun VadretRoot(
    radarTab: RadarTab?,
    systemBarsEffect: @Composable (theme: Theme) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MainViewModel = koinViewModel()
    val theme: Theme? by viewModel.theme.collectAsStateWithLifecycle()
    val language: AppLanguage? by viewModel.language.collectAsStateWithLifecycle()
    val hourFormat: HourFormat? by viewModel.hourFormat.collectAsStateWithLifecycle()
    val destinations: ImmutableList<TopLevelDestination> = remember(radarTab) { topLevelDestinations(isRadarShown = radarTab != null) }
    val tabNavigation: TabNavigation = rememberTabNavigation(destinations = destinations)
    LaunchedEffect(Unit) {
        viewModel.onEvent(event = MainEvent.OnStart)
    }
    LaunchedEffect(viewModel, tabNavigation) {
        viewModel.openedWarningIdentifiers.collect { warningIdentifiers: List<String> ->
            tabNavigation.open(key = WarningStoryKey(warningIdentifiers = warningIdentifiers))
        }
    }
    val currentTheme: Theme = theme ?: return
    val currentLanguage: AppLanguage = language ?: return
    val currentHourFormat: HourFormat = hourFormat ?: return
    systemBarsEffect(currentTheme)
    LanguageEnvironment(
        languageTag = currentLanguage.languageTag,
        hourFormat = currentHourFormat
    ) {
        VadretTheme(theme = currentTheme) {
            VadretApp(
                tabNavigation = tabNavigation,
                destinations = destinations,
                radarTab = radarTab,
                modifier = modifier
            )
        }
    }
}