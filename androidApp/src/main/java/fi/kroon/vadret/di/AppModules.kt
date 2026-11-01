package fi.kroon.vadret.di

import android.content.Context
import fi.kroon.vadret.BuildConfig
import fi.kroon.vadret.model.AppInfo
import fi.kroon.vadret.radar.di.androidRadarModule
import fi.kroon.vadret.radar.di.radarModule
import org.koin.core.module.Module

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