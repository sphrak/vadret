package se.vadretapp.vadret.main

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import org.koin.core.Koin
import se.vadretapp.vadret.BaseApplication
import se.vadretapp.vadret.BuildConfig
import se.vadretapp.vadret.di.LocalKoin
import se.vadretapp.vadret.radar.ui.RadarMapHost
import se.vadretapp.vadret.radar.ui.RadarRoute

private const val RADAR_IMAGE_URL_EXTRA = "radar_image_url"

private fun radarTab(initialRadarImageUrl: String?): RadarTab =
    RadarTab(
        host = { content: @Composable () -> Unit -> RadarMapHost(content = content) },
        content = { RadarRoute(initialImageUrl = initialRadarImageUrl) }
    )

private fun Intent.initialRadarImageUrl(): String? =
    if (BuildConfig.IS_DEVELOPER_BUILD) {
        getStringExtra(RADAR_IMAGE_URL_EXTRA)
    } else {
        null
    }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val koin: Koin = (application as BaseApplication).koinApplication.koin
        val radarTab: RadarTab = radarTab(initialRadarImageUrl = intent.initialRadarImageUrl())
        setContent {
            CompositionLocalProvider(LocalKoin provides koin) {
                VadretRoot(
                    radarTab = radarTab,
                    systemBarsEffect = { isDark: Boolean -> SystemBarsEffect(isDark = isDark) }
                )
            }
        }
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