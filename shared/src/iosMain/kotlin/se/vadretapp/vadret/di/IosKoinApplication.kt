package se.vadretapp.vadret.di

import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import platform.Foundation.NSBundle
import se.vadretapp.vadret.di.model.AppInfo
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.logging.LoggerFactory
import se.vadretapp.vadret.logging.nsLogSink
import se.vadretapp.vadret.pushalert.di.pushAlertModule
import se.vadretapp.vadret.radar.di.iosRadarModule
import se.vadretapp.vadret.radar.di.radarModule
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

private const val NS_LOG_SINK_NAME = "nslog"
private const val VERSION_KEY = "CFBundleShortVersionString"
private const val UNKNOWN_VERSION = ""
private const val APP_GROUP_KEY = "AppGroup"

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
                iosPushAlertModule(appGroup = appGroup())
            )
        )
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

// Info.plist derives the app group from the bundle id, so a build with another bundle id gets its own group.
private fun appGroup(): String =
    checkNotNull(value = NSBundle.mainBundle.objectForInfoDictionaryKey(key = APP_GROUP_KEY) as? String) { "Info.plist has no $APP_GROUP_KEY" }