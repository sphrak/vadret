package se.vadretapp.vadret.ui

import platform.Foundation.NSUserDefaults

private const val APPLE_LANGUAGES_KEY = "AppleLanguages"

internal actual fun applyLanguage(languageTag: String?) {
    val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
    if (languageTag != null) {
        userDefaults.setObject(
            value = listOf(languageTag),
            forKey = APPLE_LANGUAGES_KEY
        )
    } else {
        userDefaults.removeObjectForKey(defaultName = APPLE_LANGUAGES_KEY)
    }
}