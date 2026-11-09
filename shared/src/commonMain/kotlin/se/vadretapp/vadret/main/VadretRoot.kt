package se.vadretapp.vadret.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.main.navigation.TabNavigation
import se.vadretapp.vadret.main.navigation.TopLevelDestination
import se.vadretapp.vadret.main.navigation.WarningStoryKey
import se.vadretapp.vadret.setting.model.Theme

@Composable
fun VadretRoot(
    radarTab: RadarTab?,
    systemBarsEffect: @Composable (theme: Theme) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: MainViewModel = koinViewModel()
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
    AppEnvironment(
        mainViewModel = viewModel,
        systemBarsEffect = systemBarsEffect
    ) {
        VadretApp(
            tabNavigation = tabNavigation,
            destinations = destinations,
            radarTab = radarTab,
            modifier = modifier
        )
    }
}