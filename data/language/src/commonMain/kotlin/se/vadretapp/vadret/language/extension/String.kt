package se.vadretapp.vadret.language.extension

import se.vadretapp.vadret.language.model.AppLanguage

internal fun String?.toAppLanguage(): AppLanguage = AppLanguage.entries.firstOrNull { language: AppLanguage -> language.name == this } ?: AppLanguage.SYSTEM