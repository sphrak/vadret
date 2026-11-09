package se.vadretapp.vadret.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.rememberResourceEnvironment
import se.vadretapp.vadret.di.koinAll
import se.vadretapp.vadret.forecastwarning.ui.WarningMapHost
import se.vadretapp.vadret.main.navigation.RadarKey
import se.vadretapp.vadret.main.navigation.ScreenKind
import se.vadretapp.vadret.main.navigation.VadretKey
import se.vadretapp.vadret.main.navigation.screenKind
import se.vadretapp.vadret.radar.ui.RadarMapHost
import se.vadretapp.vadret.radar.ui.RadarRoute
import se.vadretapp.vadret.ui.DeveloperSettingsSection
import se.vadretapp.vadret.ui.LocalNativeNavigationBar
import se.vadretapp.vadret.ui.SettingsSection
import se.vadretapp.vadret.ui.VadretTheme

private val noInsets: WindowInsets = WindowInsets(
    left = 0,
    top = 0,
    right = 0,
    bottom = 0
)

private val floatingRadarTab: RadarTab = RadarTab(
    host = { content: @Composable () -> Unit -> RadarMapHost(content = content) },
    content = { placeIsland: @Composable (latitude: Double, longitude: Double, onDismiss: () -> Unit) -> Unit ->
        RadarRoute(
            placeIsland = placeIsland,
            isDockFloating = true
        )
    }
)

@Composable
internal fun NativeScreen(
    key: VadretKey,
    onNavigate: (NativeRoute) -> Unit,
    onGoBack: () -> Unit,
    onSelectTab: (NativeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val settingsSections: List<SettingsSection> = koinAll()
    val developerSettingsSections: List<DeveloperSettingsSection> = koinAll()
    val resourceEnvironment: ResourceEnvironment = rememberResourceEnvironment()
    val coroutineScope: CoroutineScope = rememberCoroutineScope()
    val appNavigator: NativeAppNavigator = remember(resourceEnvironment, coroutineScope, settingsSections, developerSettingsSections) {
        NativeAppNavigator(
            resourceEnvironment = resourceEnvironment,
            coroutineScope = coroutineScope,
            settingsSections = settingsSections,
            developerSettingsSections = developerSettingsSections,
            onNavigate = onNavigate,
            onGoBack = onGoBack,
            onSelectTab = onSelectTab
        )
    }
    val screenKind: ScreenKind = key.screenKind
    CompositionLocalProvider(LocalNativeNavigationBar provides screenKind.isNativeNavigationBarShown()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(color = VadretTheme.colorScheme.background)
                .windowInsetsPadding(insets = key.coveredInsets())
        ) {
            WarningMapHost {
                NativeRadarHost(key = key) {
                    ScreenContent(
                        key = key,
                        appNavigator = appNavigator,
                        radarTab = floatingRadarTab,
                        settingsSections = settingsSections,
                        developerSettingsSections = appNavigator.developerSettingsSections
                    )
                }
            }
        }
    }
}

@Composable
private fun NativeRadarHost(
    key: VadretKey,
    content: @Composable () -> Unit
) {
    if (key == RadarKey) {
        floatingRadarTab.host(content)
    } else {
        content()
    }
}

private fun ScreenKind.isNativeNavigationBarShown(): Boolean =
    when (this) {
        ScreenKind.DETAIL, ScreenKind.SHEET -> true
        ScreenKind.TAB, ScreenKind.DETAIL_WITH_OWN_HEADER, ScreenKind.STORY -> false
    }

@Composable
private fun VadretKey.coveredInsets(): WindowInsets =
    if (this == RadarKey) {
        noInsets
    } else {
        screenKind.coveredInsets()
    }

@Composable
private fun ScreenKind.coveredInsets(): WindowInsets =
    when (this) {
        ScreenKind.TAB, ScreenKind.DETAIL_WITH_OWN_HEADER -> WindowInsets.systemBars.only(sides = WindowInsetsSides.Bottom)
        ScreenKind.DETAIL, ScreenKind.SHEET -> WindowInsets.systemBars.only(sides = WindowInsetsSides.Top + WindowInsetsSides.Bottom)
        ScreenKind.STORY -> noInsets
    }