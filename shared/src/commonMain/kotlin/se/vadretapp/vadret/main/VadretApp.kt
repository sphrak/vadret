package se.vadretapp.vadret.main

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import se.vadretapp.vadret.di.koinAll
import se.vadretapp.vadret.forecastwarning.ui.WarningMapHost
import se.vadretapp.vadret.main.navigation.PlaceForecastKey
import se.vadretapp.vadret.main.navigation.PlaceSearchKey
import se.vadretapp.vadret.main.navigation.RadarKey
import se.vadretapp.vadret.main.navigation.ScreenKind
import se.vadretapp.vadret.main.navigation.SettingsAboutKey
import se.vadretapp.vadret.main.navigation.SettingsDeveloperKey
import se.vadretapp.vadret.main.navigation.SettingsDeveloperSectionKey
import se.vadretapp.vadret.main.navigation.SettingsKey
import se.vadretapp.vadret.main.navigation.SettingsLabsKey
import se.vadretapp.vadret.main.navigation.SettingsLicensesKey
import se.vadretapp.vadret.main.navigation.SettingsPlacesKey
import se.vadretapp.vadret.main.navigation.SettingsPollenKey
import se.vadretapp.vadret.main.navigation.SettingsSectionKey
import se.vadretapp.vadret.main.navigation.SimulatedWarningStoryKey
import se.vadretapp.vadret.main.navigation.TabNavigation
import se.vadretapp.vadret.main.navigation.TopLevelDestination
import se.vadretapp.vadret.main.navigation.VadretKey
import se.vadretapp.vadret.main.navigation.VadretNavigationBar
import se.vadretapp.vadret.main.navigation.WarningDetailKey
import se.vadretapp.vadret.main.navigation.WarningKey
import se.vadretapp.vadret.main.navigation.WarningPlacesKey
import se.vadretapp.vadret.main.navigation.WarningStoryKey
import se.vadretapp.vadret.main.navigation.WeatherKey
import se.vadretapp.vadret.main.navigation.currentTabKey
import se.vadretapp.vadret.main.navigation.goBack
import se.vadretapp.vadret.main.navigation.navigationBarPadding
import se.vadretapp.vadret.main.navigation.screenKind
import se.vadretapp.vadret.main.navigation.selectTab
import se.vadretapp.vadret.main.navigation.shownEntries
import se.vadretapp.vadret.ui.DeveloperSettingsSection
import se.vadretapp.vadret.ui.SettingsSection
import se.vadretapp.vadret.ui.VadretTheme

private val navigationConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(baseClass = NavKey::class) {
            subclass(
                subclass = WeatherKey::class,
                serializer = WeatherKey.serializer()
            )
            subclass(
                subclass = PlaceSearchKey::class,
                serializer = PlaceSearchKey.serializer()
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
                subclass = WarningPlacesKey::class,
                serializer = WarningPlacesKey.serializer()
            )
            subclass(
                subclass = WarningStoryKey::class,
                serializer = WarningStoryKey.serializer()
            )
            subclass(
                subclass = SimulatedWarningStoryKey::class,
                serializer = SimulatedWarningStoryKey.serializer()
            )
            subclass(
                subclass = RadarKey::class,
                serializer = RadarKey.serializer()
            )
            subclass(
                subclass = PlaceForecastKey::class,
                serializer = PlaceForecastKey.serializer()
            )
            subclass(
                subclass = SettingsKey::class,
                serializer = SettingsKey.serializer()
            )
            subclass(
                subclass = SettingsPlacesKey::class,
                serializer = SettingsPlacesKey.serializer()
            )
            subclass(
                subclass = SettingsSectionKey::class,
                serializer = SettingsSectionKey.serializer()
            )
            subclass(
                subclass = SettingsPollenKey::class,
                serializer = SettingsPollenKey.serializer()
            )
            subclass(
                subclass = SettingsLabsKey::class,
                serializer = SettingsLabsKey.serializer()
            )
            subclass(
                subclass = SettingsAboutKey::class,
                serializer = SettingsAboutKey.serializer()
            )
            subclass(
                subclass = SettingsLicensesKey::class,
                serializer = SettingsLicensesKey.serializer()
            )
            subclass(
                subclass = SettingsDeveloperKey::class,
                serializer = SettingsDeveloperKey.serializer()
            )
            subclass(
                subclass = SettingsDeveloperSectionKey::class,
                serializer = SettingsDeveloperSectionKey.serializer()
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
    val settingsSections: List<SettingsSection> = koinAll()
    val koinDeveloperSettingsSections: List<DeveloperSettingsSection> = koinAll()
    val developerSettingsSections: List<DeveloperSettingsSection> = remember(koinDeveloperSettingsSections, tabNavigation) {
        koinDeveloperSettingsSections.withWarningStorySimulation(appNavigator = tabNavigation)
    }
    val tabEntryProvider: (NavKey) -> NavEntry<NavKey> = { navKey: NavKey ->
        val key: VadretKey = navKey as VadretKey
        NavEntry(
            key = navKey,
            metadata = key.screenKind.transitions()
        ) {
            ComposePage(screenKind = key.screenKind) {
                ScreenContent(
                    key = key,
                    appNavigator = tabNavigation,
                    radarTab = radarTab,
                    settingsSections = settingsSections,
                    developerSettingsSections = developerSettingsSections
                )
            }
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
        WarningMapHost {
            RadarHost(radarTab = radarTab) {
                NavDisplay(
                    entries = tabNavigation.shownEntries(tabEntries = tabEntries),
                    onBack = { tabNavigation.goBack() },
                    transitionSpec = { instantTransition() },
                    popTransitionSpec = { instantTransition() },
                    predictivePopTransitionSpec = { instantTransition() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ComposePage(
    screenKind: ScreenKind,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    when (screenKind) {
        ScreenKind.TAB, ScreenKind.DETAIL, ScreenKind.DETAIL_WITH_OWN_HEADER -> TabPage(
            paddingValues = navigationBarPadding(),
            modifier = modifier,
            content = content
        )
        ScreenKind.SHEET -> SheetPage(
            modifier = modifier,
            content = content
        )
        ScreenKind.STORY -> Box(modifier = modifier) {
            content()
        }
    }
}

// Screens slide over each other and a back gesture draws two at once, so each page covers the one beneath it.
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
            .background(color = VadretTheme.colorScheme.background)
    ) {
        content()
    }
}

// A sheet covers the tab bar too, so it only keeps clear of the system navigation.
@Composable
private fun SheetPage(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = VadretTheme.colorScheme.background)
            .navigationBarsPadding()
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

private fun slideOutToEnd(): ContentTransform = EnterTransition.None togetherWith slideOutHorizontally(targetOffsetX = { width: Int -> width })

private fun ScreenKind.transitions(): Map<String, Any> =
    when (this) {
        ScreenKind.TAB -> emptyMap()
        ScreenKind.DETAIL, ScreenKind.DETAIL_WITH_OWN_HEADER -> detailTransitions
        ScreenKind.SHEET, ScreenKind.STORY -> storyTransitions
    }

private val storyTransitions: Map<String, Any> =
    NavDisplay.transitionSpec {
        slideInVertically(initialOffsetY = { height: Int -> height }) togetherWith ExitTransition.KeepUntilTransitionsFinished
    } + NavDisplay.popTransitionSpec { slideDown() } + NavDisplay.predictivePopTransitionSpec { _: Int -> slideDown() }

private val detailTransitions: Map<String, Any> =
    NavDisplay.transitionSpec {
        slideInHorizontally(initialOffsetX = { width: Int -> width }) togetherWith ExitTransition.KeepUntilTransitionsFinished
    } + NavDisplay.popTransitionSpec { slideOutToEnd() } + NavDisplay.predictivePopTransitionSpec { _: Int -> slideOutToEnd() }