package se.vadretapp.vadret.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController
import se.vadretapp.vadret.di.LocalKoin
import se.vadretapp.vadret.di.koinApplication
import se.vadretapp.vadret.radar.ui.RadarMapHost
import se.vadretapp.vadret.radar.ui.RadarRoute

private val radarTab = RadarTab(
    host = { content: @Composable () -> Unit -> RadarMapHost(content = content) },
    content = { placeIsland: @Composable (latitude: Double, longitude: Double, onDismiss: () -> Unit) -> Unit ->
        RadarRoute(placeIsland = placeIsland)
    }
)

fun mainViewController(): UIViewController =
    ComposeUIViewController {
        CompositionLocalProvider(LocalKoin provides koinApplication.koin) {
            VadretRoot(
                radarTab = radarTab,
                systemBarsEffect = { isDark: Boolean -> InterfaceStyleEffect(isDark = isDark) }
            )
        }
    }

@Composable
private fun InterfaceStyleEffect(isDark: Boolean) {
    val viewController: UIViewController = LocalUIViewController.current
    LaunchedEffect(viewController, isDark) {
        viewController.overrideUserInterfaceStyle = if (isDark) {
            UIUserInterfaceStyle.UIUserInterfaceStyleDark
        } else {
            UIUserInterfaceStyle.UIUserInterfaceStyleLight
        }
    }
}