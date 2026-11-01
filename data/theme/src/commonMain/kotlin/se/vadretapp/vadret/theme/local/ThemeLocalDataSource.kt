package se.vadretapp.vadret.theme.local

import kotlinx.coroutines.flow.Flow

internal interface ThemeLocalDataSource {
    suspend fun getTheme(): String?
    fun observeTheme(): Flow<String?>
    suspend fun setTheme(value: String)
}