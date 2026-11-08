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
import se.vadretapp.vadret.setting.model.Theme

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
                systemBarsEffect = { theme: Theme -> InterfaceStyleEffect(theme = theme) }
            )
        }
    }

@Composable
private fun InterfaceStyleEffect(theme: Theme) {
    val viewController: UIViewController = LocalUIViewController.current
    LaunchedEffect(viewController, theme) {
        viewController.overrideUserInterfaceStyle = theme.toInterfaceStyle()
    }
}

// Following the system leaves the style unset, since an override would also hide the system's own switches
// from the app.
private fun Theme.toInterfaceStyle(): UIUserInterfaceStyle =
    when (this) {
        Theme.LIGHT -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
        Theme.DARK -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
        Theme.AMOLED -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
        Theme.SYSTEM -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
    }