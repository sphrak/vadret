package se.vadretapp.vadret.ui

import android.content.res.Resources
import java.util.Locale

actual fun applyLanguage(languageTag: String?) {
    val locale: Locale = if (languageTag != null) {
        Locale.forLanguageTag(languageTag)
    } else {
        Resources.getSystem().configuration.locales[0]
    }
    Locale.setDefault(locale)
}