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
import se.vadretapp.vadret.di.koinAll
import se.vadretapp.vadret.forecastwarning.ui.WarningDetailRoute
import se.vadretapp.vadret.forecastwarning.ui.WarningMapHost
import se.vadretapp.vadret.forecastwarning.ui.WarningRoute
import se.vadretapp.vadret.forecastwarning.ui.WarningStoryRoute
import se.vadretapp.vadret.main.navigation.PlaceForecastKey
import se.vadretapp.vadret.main.navigation.RadarKey
import se.vadretapp.vadret.main.navigation.SettingsAboutKey
import se.vadretapp.vadret.main.navigation.SettingsDeveloperKey
import se.vadretapp.vadret.main.navigation.SettingsDeveloperSectionKey
import se.vadretapp.vadret.main.navigation.SettingsKey
import se.vadretapp.vadret.main.navigation.SettingsLabsKey
import se.vadretapp.vadret.main.navigation.SettingsLicensesKey
import se.vadretapp.vadret.main.navigation.SettingsPlacesKey
import se.vadretapp.vadret.main.navigation.SettingsPollenKey
import se.vadretapp.vadret.main.navigation.SettingsSectionKey
import se.vadretapp.vadret.main.navigation.TabNavigation
import se.vadretapp.vadret.main.navigation.TopLevelDestination
import se.vadretapp.vadret.main.navigation.VadretNavigationBar
import se.vadretapp.vadret.main.navigation.WarningDetailKey
import se.vadretapp.vadret.main.navigation.WarningKey
import se.vadretapp.vadret.main.navigation.WarningStoryKey
import se.vadretapp.vadret.main.navigation.WeatherKey
import se.vadretapp.vadret.main.navigation.close
import se.vadretapp.vadret.main.navigation.closePlaceForecast
import se.vadretapp.vadret.main.navigation.currentTabKey
import se.vadretapp.vadret.main.navigation.goBack
import se.vadretapp.vadret.main.navigation.navigationBarPadding
import se.vadretapp.vadret.main.navigation.open
import se.vadretapp.vadret.main.navigation.openOnTab
import se.vadretapp.vadret.main.navigation.openPlaceForecast
import se.vadretapp.vadret.main.navigation.openWeatherTab
import se.vadretapp.vadret.main.navigation.selectTab
import se.vadretapp.vadret.main.navigation.shownEntries
import se.vadretapp.vadret.settings.ui.AboutRoute
import se.vadretapp.vadret.settings.ui.DeveloperRoute
import se.vadretapp.vadret.settings.ui.LabsRoute
import se.vadretapp.vadret.settings.ui.LicensesRoute
import se.vadretapp.vadret.settings.ui.PlacesRoute
import se.vadretapp.vadret.settings.ui.PollenSettingsRoute
import se.vadretapp.vadret.settings.ui.SettingsRoute
import se.vadretapp.vadret.ui.DeveloperSettingsSection
import se.vadretapp.vadret.ui.SettingsSection
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.weatherforecast.ui.PlaceForecastRoute
import se.vadretapp.vadret.weatherforecast.ui.PlaceIslandRoute
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
    val tabPadding: PaddingValues = navigationBarPadding()
    val settingsSections: List<SettingsSection> = koinAll()
    val settingsSectionsByKey: Map<String, SettingsSection> = remember(settingsSections) {
        settingsSections.associateBy { settingsSection: SettingsSection -> settingsSection.key }
    }
    val developerSettingsSections: List<DeveloperSettingsSection> = koinAll()
    val developerSettingsSectionsByKey: Map<String, DeveloperSettingsSection> = remember(developerSettingsSections) {
        developerSettingsSections.associateBy { developerSettingsSection: DeveloperSettingsSection -> developerSettingsSection.key }
    }
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
        entry<PlaceForecastKey>(metadata = detailTransitions) { placeForecastKey: PlaceForecastKey ->
            TabPage(paddingValues = tabPadding) {
                PlaceForecastRoute(
                    latitude = placeForecastKey.latitude,
                    longitude = placeForecastKey.longitude,
                    onClose = { tabNavigation.closePlaceForecast(placeForecastKey = placeForecastKey) },
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
                    },
                    onFollowedPlacesClick = {
                        tabNavigation.openOnTab(
                            tabKey = SettingsKey,
                            key = SettingsPlacesKey
                        )
                    }
                )
            }
        }
        entry<WarningDetailKey>(metadata = detailTransitions) { warningDetailKey: WarningDetailKey ->
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
                TabPage(paddingValues = tabPadding) {
                    radarTab.content { latitude: Double, longitude: Double, onDismiss: () -> Unit ->
                        PlaceIslandRoute(
                            latitude = latitude,
                            longitude = longitude,
                            onDismiss = onDismiss,
                            onOpenForecast = {
                                tabNavigation.openPlaceForecast(
                                    placeForecastKey = PlaceForecastKey(
                                        latitude = latitude,
                                        longitude = longitude
                                    )
                                )
                            },
                            onOpenChosenLocation = {
                                onDismiss()
                                tabNavigation.openWeatherTab()
                            }
                        )
                    }
                }
            }
        }
        entry<SettingsKey> {
            TabPage(paddingValues = tabPadding) {
                SettingsRoute(
                    onPlacesClick = { tabNavigation.open(key = SettingsPlacesKey) },
                    onSectionClick = { sectionKey: String -> tabNavigation.open(key = SettingsSectionKey(sectionKey = sectionKey)) },
                    onPollenClick = { tabNavigation.open(key = SettingsPollenKey) },
                    onLabsClick = { tabNavigation.open(key = SettingsLabsKey) },
                    onDeveloperClick = { tabNavigation.open(key = SettingsDeveloperKey) },
                    onAboutClick = { tabNavigation.open(key = SettingsAboutKey) },
                    settingsSections = settingsSections
                )
            }
        }
        entry<SettingsPlacesKey>(metadata = detailTransitions) {
            TabPage(paddingValues = tabPadding) {
                PlacesRoute(onBack = { tabNavigation.close(key = SettingsPlacesKey) })
            }
        }
        entry<SettingsSectionKey>(metadata = detailTransitions) { settingsSectionKey: SettingsSectionKey ->
            TabPage(paddingValues = tabPadding) {
                settingsSectionsByKey[settingsSectionKey.sectionKey]?.Content(onBack = { tabNavigation.close(key = settingsSectionKey) })
            }
        }
        entry<SettingsPollenKey>(metadata = detailTransitions) {
            TabPage(paddingValues = tabPadding) {
                PollenSettingsRoute(onBack = { tabNavigation.close(key = SettingsPollenKey) })
            }
        }
        entry<SettingsLabsKey>(metadata = detailTransitions) {
            TabPage(paddingValues = tabPadding) {
                LabsRoute(onBack = { tabNavigation.close(key = SettingsLabsKey) })
            }
        }
        entry<SettingsAboutKey>(metadata = detailTransitions) {
            TabPage(paddingValues = tabPadding) {
                AboutRoute(
                    onLicensesClick = { tabNavigation.open(key = SettingsLicensesKey) },
                    onBack = { tabNavigation.close(key = SettingsAboutKey) }
                )
            }
        }
        entry<SettingsLicensesKey>(metadata = detailTransitions) {
            TabPage(paddingValues = tabPadding) {
                LicensesRoute(onBack = { tabNavigation.close(key = SettingsLicensesKey) })
            }
        }
        entry<SettingsDeveloperKey>(metadata = detailTransitions) {
            TabPage(paddingValues = tabPadding) {
                DeveloperRoute(
                    onSectionClick = { sectionKey: String -> tabNavigation.open(key = SettingsDeveloperSectionKey(sectionKey = sectionKey)) },
                    onBack = { tabNavigation.close(key = SettingsDeveloperKey) },
                    developerSettingsSections = developerSettingsSections
                )
            }
        }
        entry<SettingsDeveloperSectionKey>(metadata = detailTransitions) { settingsDeveloperSectionKey: SettingsDeveloperSectionKey ->
            TabPage(paddingValues = tabPadding) {
                developerSettingsSectionsByKey[settingsDeveloperSectionKey.sectionKey]?.Content(onBack = { tabNavigation.close(key = settingsDeveloperSectionKey) })
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

private val storyTransitions: Map<String, Any> =
    NavDisplay.transitionSpec {
        slideInVertically(initialOffsetY = { height: Int -> height }) togetherWith ExitTransition.KeepUntilTransitionsFinished
    } + NavDisplay.popTransitionSpec { slideDown() } + NavDisplay.predictivePopTransitionSpec { _: Int -> slideDown() }

private val detailTransitions: Map<String, Any> =
    NavDisplay.transitionSpec {
        slideInHorizontally(initialOffsetX = { width: Int -> width }) togetherWith ExitTransition.KeepUntilTransitionsFinished
    } + NavDisplay.popTransitionSpec { slideOutToEnd() } + NavDisplay.predictivePopTransitionSpec { _: Int -> slideOutToEnd() }