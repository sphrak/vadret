package se.vadretapp.vadret.setting

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import se.vadretapp.vadret.setting.local.SettingLocalDataSource
import se.vadretapp.vadret.setting.local.SystemSettingLocalDataSource

internal class FakeSystemSettingLocalDataSource(
    var isTwentyFourHourClock: Boolean = true,
    var systemLanguageTag: String = "sv-SE"
) : SystemSettingLocalDataSource {
    override fun isTwentyFourHour(): Boolean = isTwentyFourHourClock
    override fun languageTag(): String = systemLanguageTag
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