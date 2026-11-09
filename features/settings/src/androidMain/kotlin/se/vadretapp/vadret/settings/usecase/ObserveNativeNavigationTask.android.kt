package se.vadretapp.vadret.settings.usecase

import se.vadretapp.vadret.setting.SettingRepository

internal actual fun ObserveNativeNavigationTask(settingRepository: SettingRepository): ObserveNativeNavigationTask = UnavailableObserveNativeNavigationTask()