package se.vadretapp.vadret.ui.format

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.Foundation.preferredLanguages

private const val IDENTIFIER_SEPARATOR = '_'
private const val TAG_SEPARATOR = '-'

actual fun currentLanguageTag(): String {
    val preferredLanguage: String? = NSLocale.preferredLanguages.firstOrNull() as? String
    val identifier: String = preferredLanguage ?: NSLocale.currentLocale.localeIdentifier
    return identifier.replace(
        oldChar = IDENTIFIER_SEPARATOR,
        newChar = TAG_SEPARATOR
    )
}