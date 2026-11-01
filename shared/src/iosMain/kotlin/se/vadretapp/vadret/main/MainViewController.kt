package se.vadretapp.vadret.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.window.ComposeUIViewController
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import platform.Foundation.NSBundle
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController
import se.vadretapp.vadret.di.LocalKoin
import se.vadretapp.vadret.di.iosPlatformModule
import se.vadretapp.vadret.di.iosPushAlertModule
import se.vadretapp.vadret.di.sharedModules
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.logging.nsLogSink
import se.vadretapp.vadret.model.AppInfo
import se.vadretapp.vadret.pushalert.di.pushAlertModule
import se.vadretapp.vadret.radar.di.iosRadarModule
import se.vadretapp.vadret.radar.di.radarModule
import se.vadretapp.vadret.radar.ui.RadarMapHost
import se.vadretapp.vadret.radar.ui.RadarRoute
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

private const val NS_LOG_SINK_NAME = "nslog"
private const val VERSION_KEY = "CFBundleShortVersionString"
private const val UNKNOWN_VERSION = ""

private val radarTab = RadarTab(
    host = { content: @Composable () -> Unit -> RadarMapHost(content = content) },
    content = { RadarRoute() }
)

internal val koinApplication: KoinApplication by lazy {
    val appInfo: AppInfo = appInfo()
    if (appInfo.isDebug) {
        LoggerFactory.register(
            name = NS_LOG_SINK_NAME,
            sink = nsLogSink(minimumLevel = LogLevel.DEBUG)
        )
    }
    koinApplication {
        modules(
            modules = sharedModules + listOf(
                iosPlatformModule(appInfo = appInfo),
                radarModule,
                iosRadarModule,
                pushAlertModule,
                iosPushAlertModule
            )
        )
    }
}

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

@OptIn(ExperimentalNativeApi::class)
private fun appInfo(): AppInfo =
    AppInfo(
        applicationId = NSBundle.mainBundle.bundleIdentifier.orEmpty(),
        versionName = NSBundle.mainBundle.objectForInfoDictionaryKey(key = VERSION_KEY) as? String ?: UNKNOWN_VERSION,
        isDebug = Platform.isDebugBinary,
        isDeveloperBuild = Platform.isDebugBinary
    )