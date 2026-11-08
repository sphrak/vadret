package se.vadretapp.vadret.setting

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import se.vadretapp.vadret.setting.local.IosSystemSettingLocalDataSource

fun SettingRepository(dataStore: DataStore<Preferences>): SettingRepository =
    SettingRepository(
        dataStore = dataStore,
        systemSettingLocalDataSource = IosSystemSettingLocalDataSource()
    )