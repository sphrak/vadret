package se.vadretapp.vadret.di

import android.content.Context
import org.koin.core.module.Module
import se.vadretapp.vadret.BuildConfig
import se.vadretapp.vadret.model.AppInfo
import se.vadretapp.vadret.radar.di.androidRadarModule
import se.vadretapp.vadret.radar.di.radarModule

internal fun appModules(context: Context): List<Module> =
    sharedModules + listOf(
        androidPlatformModule(
            context = context,
            appInfo = AppInfo(
                applicationId = BuildConfig.APPLICATION_ID,
                versionName = BuildConfig.APP_VERSION,
                isDebug = BuildConfig.DEBUG,
                isDeveloperBuild = BuildConfig.IS_DEVELOPER_BUILD
            )
        ),
        radarModule,
        androidRadarModule
    ) + flavorModules(context = context)