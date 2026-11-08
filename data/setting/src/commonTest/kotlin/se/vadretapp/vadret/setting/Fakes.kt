package se.vadretapp.vadret.setting

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import se.vadretapp.vadret.setting.local.SettingLocalDataSource
import se.vadretapp.vadret.setting.local.SystemSettingLocalDataSource
import se.vadretapp.vadret.setting.model.SystemSettingsEntity

internal class FakeSystemSettingLocalDataSource(
    systemSettingsEntity: SystemSettingsEntity = SystemSettingsEntity(
        isTwentyFourHour = true,
        languageTag = "sv-SE"
    )
) : SystemSettingLocalDataSource {
    val systemSettingsEntity: MutableStateFlow<SystemSettingsEntity> = MutableStateFlow(value = systemSettingsEntity)
    override fun getSystemSettings(): SystemSettingsEntity = systemSettingsEntity.value
    override fun observeSystemSettings(): Flow<SystemSettingsEntity> = systemSettingsEntity
}

internal class FailingSystemSettingLocalDataSource(
    private val exception: Exception
) : SystemSettingLocalDataSource {
    override fun getSystemSettings(): SystemSettingsEntity = throw exception
    override fun observeSystemSettings(): Flow<SystemSettingsEntity> = flow { throw exception }
}

internal class FailingSettingLocalDataSource(
    private val exception: Exception
) : SettingLocalDataSource {
    override suspend fun getTheme(): String? = throw exception
    override fun observeTheme(): Flow<String?> = flow { throw exception }
    override suspend fun setTheme(value: String) = throw exception
    override suspend fun getLanguage(): String? = throw exception
    override fun observeLanguage(): Flow<String?> = flow { throw exception }
    override suspend fun setLanguage(value: String) = throw exception
    override suspend fun getHourFormatChoice(): String? = throw exception
    override fun observeHourFormatChoice(): Flow<String?> = flow { throw exception }
    override suspend fun setHourFormatChoice(value: String) = throw exception
}