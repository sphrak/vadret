package se.vadretapp.vadret.setting

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import se.vadretapp.vadret.setting.local.AndroidSystemSettingLocalDataSource

fun SettingRepository(
    dataStore: DataStore<Preferences>,
    context: Context
): SettingRepository =
    SettingRepository(
        dataStore = dataStore,
        systemSettingLocalDataSource = AndroidSystemSettingLocalDataSource(context = context)
    )