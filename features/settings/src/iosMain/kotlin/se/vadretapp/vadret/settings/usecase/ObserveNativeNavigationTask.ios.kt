package se.vadretapp.vadret.settings.usecase

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo
import se.vadretapp.vadret.setting.SettingRepository

private const val LIQUID_GLASS_MAJOR_VERSION: Long = 26

@OptIn(ExperimentalForeignApi::class)
internal actual fun ObserveNativeNavigationTask(settingRepository: SettingRepository): ObserveNativeNavigationTask {
    val liquidGlassVersion = cValue<NSOperatingSystemVersion> { majorVersion = LIQUID_GLASS_MAJOR_VERSION }
    return if (NSProcessInfo.processInfo.isOperatingSystemAtLeastVersion(version = liquidGlassVersion)) {
        DefaultObserveNativeNavigationTask(settingRepository = settingRepository)
    } else {
        UnavailableObserveNativeNavigationTask()
    }
}