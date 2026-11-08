package se.vadretapp.vadret.setting.local

import kotlinx.coroutines.flow.Flow
import se.vadretapp.vadret.setting.model.SystemSettingsEntity

internal interface SystemSettingLocalDataSource {
    fun getSystemSettings(): SystemSettingsEntity
    fun observeSystemSettings(): Flow<SystemSettingsEntity>
}