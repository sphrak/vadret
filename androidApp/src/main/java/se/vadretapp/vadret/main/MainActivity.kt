package se.vadretapp.vadret.main

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import org.koin.core.Koin
import se.vadretapp.vadret.BaseApplication
import se.vadretapp.vadret.BuildConfig
import se.vadretapp.vadret.aggregatedfeed.model.WarningReference
import se.vadretapp.vadret.di.LocalKoin
import se.vadretapp.vadret.radar.ui.RadarMapHost
import se.vadretapp.vadret.radar.ui.RadarRoute
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.isDark

private const val RADAR_IMAGE_URL_EXTRA = "radar_image_url"
private const val LANGUAGE_TAG_EXTRA = "language_tag"

private fun radarTab(initialRadarImageUrl: String?): RadarTab =
    RadarTab(
        host = { content: @Composable () -> Unit -> RadarMapHost(content = content) },
        content = { placeIsland: @Composable (latitude: Double, longitude: Double, onDismiss: () -> Unit) -> Unit ->
            RadarRoute(
                placeIsland = placeIsland,
                initialImageUrl = initialRadarImageUrl
            )
        }
    )

private fun Intent.developerExtra(name: String): String? =
    if (BuildConfig.IS_DEVELOPER_BUILD) {
        getStringExtra(name)
    } else {
        null
    }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val koin: Koin = (application as BaseApplication).koinApplication.koin
        if (savedInstanceState == null) {
            openWarningReference(intent = intent)
        }
        val radarTab: RadarTab = radarTab(initialRadarImageUrl = intent.developerExtra(name = RADAR_IMAGE_URL_EXTRA))
        val languageTagOverride: String? = intent.developerExtra(name = LANGUAGE_TAG_EXTRA)
        setContent {
            CompositionLocalProvider(LocalKoin provides koin) {
                VadretRoot(
                    radarTab = radarTab,
                    languageTagOverride = languageTagOverride,
                    systemBarsEffect = { theme: Theme ->
                        SystemBarsEffect(isDark = theme.isDark(isSystemDark = isSystemInDarkTheme()))
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openWarningReference(intent = intent)
    }

    private fun openWarningReference(intent: Intent) {
        val warningReference: WarningReference = intent.warningReferenceOrNull() ?: return
        (application as BaseApplication).koinApplication.koin.get<OpenedWarningReferences>().open(warningReference = warningReference)
    }

    @Composable
    private fun SystemBarsEffect(isDark: Boolean) {
        DisposableEffect(isDark) {
            enableEdgeToEdge(
                statusBarStyle = systemBarStyle(isDark = isDark),
                navigationBarStyle = systemBarStyle(isDark = isDark)
            )
            onDispose {}
        }
    }

    private fun systemBarStyle(isDark: Boolean): SystemBarStyle =
        if (isDark) {
            SystemBarStyle.dark(scrim = Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(
                scrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            )
        }
}