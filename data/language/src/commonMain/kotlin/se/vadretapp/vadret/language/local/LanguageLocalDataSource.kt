package se.vadretapp.vadret.language.local

import kotlinx.coroutines.flow.Flow

internal interface LanguageLocalDataSource {
    suspend fun getLanguage(): String?
    fun observeLanguage(): Flow<String?>
    suspend fun setLanguage(value: String)
}