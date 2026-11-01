package se.vadretapp.vadret.main

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import se.vadretapp.vadret.forecastwarning.ui.WarningDetailRoute
import se.vadretapp.vadret.forecastwarning.ui.WarningRoute
import se.vadretapp.vadret.forecastwarning.ui.WarningStoryRoute
import se.vadretapp.vadret.main.navigation.RadarKey
import se.vadretapp.vadret.main.navigation.SettingsKey
import se.vadretapp.vadret.main.navigation.TabNavigation
import se.vadretapp.vadret.main.navigation.TopLevelDestination
import se.vadretapp.vadret.main.navigation.VadretNavigationBar
import se.vadretapp.vadret.main.navigation.WarningDetailKey
import se.vadretapp.vadret.main.navigation.WarningKey
import se.vadretapp.vadret.main.navigation.WarningStoryKey
import se.vadretapp.vadret.main.navigation.WeatherKey
import se.vadretapp.vadret.main.navigation.close
import se.vadretapp.vadret.main.navigation.currentTabKey
import se.vadretapp.vadret.main.navigation.goBack
import se.vadretapp.vadret.main.navigation.navigationBarPadding
import se.vadretapp.vadret.main.navigation.open
import se.vadretapp.vadret.main.navigation.selectTab
import se.vadretapp.vadret.settings.ui.SettingsRoute
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.weatherforecast.ui.WeatherForecastRoute

private val navigationConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(baseClass = NavKey::class) {
            subclass(
                subclass = WeatherKey::class,
                serializer = WeatherKey.serializer()
            )
            subclass(
                subclass = WarningKey::class,
                serializer = WarningKey.serializer()
            )
            subclass(
                subclass = WarningDetailKey::class,
                serializer = WarningDetailKey.serializer()
            )
            subclass(
                subclass = WarningStoryKey::class,
                serializer = WarningStoryKey.serializer()
            )
            subclass(
                subclass = RadarKey::class,
                serializer = RadarKey.serializer()
            )
            subclass(
                subclass = SettingsKey::class,
                serializer = SettingsKey.serializer()
            )
        }
    }
}

@Composable
internal fun rememberTabNavigation(destinations: ImmutableList<TopLevelDestination>): TabNavigation {
    val tabHistory: NavBackStack<NavKey> = rememberNavBackStack(navigationConfiguration, destinations.first().key)
    val tabBackStacks: Map<NavKey, NavBackStack<NavKey>> = destinations.associate { destination: TopLevelDestination ->
        key(destination) {
            destination.key to rememberNavBackStack(navigationConfiguration, destination.key)
        }
    }
    return remember(tabHistory, tabBackStacks) {
        TabNavigation(
            tabHistory = tabHistory,
            tabBackStacks = tabBackStacks
        )
    }
}

internal fun topLevelDestinations(isRadarShown: Boolean): ImmutableList<TopLevelDestination> =
    TopLevelDestination.entries
        .filter { destination: TopLevelDestination -> destination != TopLevelDestination.RADAR || isRadarShown }
        .toImmutableList()

@Composable
internal fun VadretApp(
    tabNavigation: TabNavigation,
    destinations: ImmutableList<TopLevelDestination>,
    radarTab: RadarTab?,
    modifier: Modifier = Modifier
) {
    val tabPadding: PaddingValues = navigationBarPadding()
    val tabEntryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        entry<WeatherKey> {
            TabPage(paddingValues = tabPadding) {
                WeatherForecastRoute(
                    onWarningClick = { warningIdentifiers: List<String> ->
                        tabNavigation.open(key = WarningStoryKey(warningIdentifiers = warningIdentifiers))
                    }
                )
            }
        }
        entry<WarningKey> {
            TabPage(paddingValues = tabPadding) {
                WarningRoute(
                    onWarningClick = { warningIdentifier: String ->
                        tabNavigation.open(key = WarningDetailKey(warningIdentifier = warningIdentifier))
                    }
                )
            }
        }
        entry<WarningDetailKey> { warningDetailKey: WarningDetailKey ->
            TabPage(paddingValues = tabPadding) {
                WarningDetailRoute(
                    warningIdentifier = warningDetailKey.warningIdentifier,
                    onBack = { tabNavigation.close(key = warningDetailKey) }
                )
            }
        }
        entry<WarningStoryKey>(metadata = storyTransitions) { warningStoryKey: WarningStoryKey ->
            WarningStoryRoute(
                warningIdentifiers = warningStoryKey.warningIdentifiers,
                onDismiss = { tabNavigation.close(key = warningStoryKey) }
            )
        }
        if (radarTab != null) {
            entry<RadarKey> {
                TabPage(paddingValues = tabPadding) { radarTab.content() }
            }
        }
        entry<SettingsKey> {
            TabPage(paddingValues = tabPadding) { SettingsRoute() }
        }
    }
    val tabEntries: Map<NavKey, List<NavEntry<NavKey>>> = tabNavigation.tabBackStacks.mapValues { tabBackStack: Map.Entry<NavKey, NavBackStack<NavKey>> ->
        key(tabBackStack.key) {
            rememberDecoratedNavEntries(
                backStack = tabBackStack.value,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                entryProvider = tabEntryProvider
            )
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = VadretTheme.colorScheme.background)
    ) {
        VadretNavigationBar(
            destinations = destinations,
            currentKey = tabNavigation.currentTabKey,
            onDestinationClick = { destination: TopLevelDestination -> tabNavigation.selectTab(tabKey = destination.key) },
            modifier = Modifier.align(alignment = Alignment.BottomCenter)
        )
        RadarHost(radarTab = radarTab) {
            NavDisplay(
                entries = tabNavigation.tabHistory.flatMap { tabKey: NavKey -> tabEntries.getValue(key = tabKey) },
                onBack = { tabNavigation.goBack() },
                transitionSpec = { instantTransition() },
                popTransitionSpec = { instantTransition() },
                predictivePopTransitionSpec = { instantTransition() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun TabPage(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .padding(paddingValues = paddingValues)
            .consumeWindowInsets(paddingValues = paddingValues)
            .clipToBounds()
    ) {
        content()
    }
}

@Composable
private fun RadarHost(
    radarTab: RadarTab?,
    content: @Composable () -> Unit
) {
    if (radarTab != null) {
        radarTab.host(content)
    } else {
        content()
    }
}

private fun instantTransition(): ContentTransform = EnterTransition.None togetherWith ExitTransition.None

private fun slideDown(): ContentTransform = EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { height: Int -> height })

private val storyTransitions: Map<String, Any> =
    NavDisplay.transitionSpec {
        slideInVertically(initialOffsetY = { height: Int -> height }) togetherWith ExitTransition.KeepUntilTransitionsFinished
    } + NavDisplay.popTransitionSpec { slideDown() } + NavDisplay.predictivePopTransitionSpec { _: Int -> slideDown() }