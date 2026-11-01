package se.vadretapp.vadret.settings.extension

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.language_danish
import se.vadretapp.vadret.core.resources.language_english
import se.vadretapp.vadret.core.resources.language_finnish
import se.vadretapp.vadret.core.resources.language_french
import se.vadretapp.vadret.core.resources.language_german
import se.vadretapp.vadret.core.resources.language_norwegian
import se.vadretapp.vadret.core.resources.language_polish
import se.vadretapp.vadret.core.resources.language_swedish
import se.vadretapp.vadret.core.resources.system_language
import se.vadretapp.vadret.language.model.AppLanguage

internal fun AppLanguage.labelResource(): StringResource =
    when (this) {
        AppLanguage.SYSTEM -> Res.string.system_language
        AppLanguage.ENGLISH -> Res.string.language_english
        AppLanguage.SWEDISH -> Res.string.language_swedish
        AppLanguage.DANISH -> Res.string.language_danish
        AppLanguage.GERMAN -> Res.string.language_german
        AppLanguage.FINNISH -> Res.string.language_finnish
        AppLanguage.FRENCH -> Res.string.language_french
        AppLanguage.NORWEGIAN -> Res.string.language_norwegian
        AppLanguage.POLISH -> Res.string.language_polish
    }