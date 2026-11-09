package se.vadretapp.vadret.setting.local

import kotlinx.coroutines.flow.Flow

internal interface SettingLocalDataSource {
    suspend fun getTheme(): String?
    fun observeTheme(): Flow<String?>
    suspend fun setTheme(value: String)
    suspend fun getLanguage(): String?
    fun observeLanguage(): Flow<String?>
    suspend fun setLanguage(value: String)
    suspend fun getHourFormatChoice(): String?
    fun observeHourFormatChoice(): Flow<String?>
    suspend fun setHourFormatChoice(value: String)
    suspend fun getNativeNavigationEnabled(): Boolean?
    fun observeNativeNavigationEnabled(): Flow<Boolean?>
    suspend fun setNativeNavigationEnabled(value: Boolean)
}