package se.vadretapp.vadret.setting.extension

import se.vadretapp.vadret.setting.model.AppLanguage

private const val TAG_SEPARATOR = '-'
private val norwegianWrittenLanguages: Set<String> = setOf("nb", "nn")

internal fun AppLanguage.toPreferenceValue(): String = name

// The app shows the phone's language when it has it and English otherwise, so that is the language followed.
internal fun AppLanguage.toShownLanguage(systemLanguageTag: String): AppLanguage {
    if (this != AppLanguage.SYSTEM) {
        return this
    }
    val systemLanguage: String = systemLanguageTag.substringBefore(delimiter = TAG_SEPARATOR).lowercase()
    val primaryLanguage: String? = if (systemLanguage in norwegianWrittenLanguages) {
        AppLanguage.NORWEGIAN.primaryLanguage()
    } else {
        systemLanguage
    }
    return AppLanguage.entries.firstOrNull { language: AppLanguage -> language.primaryLanguage() == primaryLanguage } ?: AppLanguage.ENGLISH
}

private fun AppLanguage.primaryLanguage(): String? = languageTag?.substringBefore(delimiter = TAG_SEPARATOR)