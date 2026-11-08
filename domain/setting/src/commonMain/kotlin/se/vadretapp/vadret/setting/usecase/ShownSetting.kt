package se.vadretapp.vadret.setting.usecase

import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.HourFormatChoice
import se.vadretapp.vadret.setting.model.Language

internal fun HourFormatChoice.toHourFormat(isSystemTwentyFourHour: Boolean): HourFormat =
    when (this) {
        HourFormatChoice.SYSTEM -> if (isSystemTwentyFourHour) {
            HourFormat.TWENTY_FOUR_HOUR
        } else {
            HourFormat.TWELVE_HOUR
        }
        HourFormatChoice.TWENTY_FOUR_HOUR -> HourFormat.TWENTY_FOUR_HOUR
        HourFormatChoice.TWELVE_HOUR -> HourFormat.TWELVE_HOUR
    }

// The app's texts are kept per language and region, and a phone whose language and region match none of them reads
// the English ones. The language the app shows is matched the same way, so pushes come in the language of the app.
internal fun AppLanguage.toShownLanguage(systemLanguageTag: String): Language =
    when (this) {
        AppLanguage.SYSTEM -> systemLanguageTag.toShownLanguage()
        AppLanguage.ENGLISH -> Language.ENGLISH
        AppLanguage.SWEDISH -> Language.SWEDISH
        AppLanguage.DANISH -> Language.DANISH
        AppLanguage.GERMAN -> Language.GERMAN
        AppLanguage.FINNISH -> Language.FINNISH
        AppLanguage.FRENCH -> Language.FRENCH
        AppLanguage.NORWEGIAN -> Language.NORWEGIAN
        AppLanguage.POLISH -> Language.POLISH
    }

private fun String.toShownLanguage(): Language {
    val systemLocale: LanguageAndRegion = toLanguageAndRegion()
    return Language.entries.firstOrNull { language: Language -> language.languageTag.toLanguageAndRegion() == systemLocale }
        ?: Language.entries.firstOrNull { language: Language ->
            val shownLocale: LanguageAndRegion = language.languageTag.toLanguageAndRegion()
            shownLocale.region == null && shownLocale.language == systemLocale.language
        }
        ?: Language.ENGLISH
}

private data class LanguageAndRegion(
    val language: String,
    val region: String?
)

// A tag such as sv-Latn-SE-u-mu-celsius is the language, a script, the region and then extensions after a single letter.
private fun String.toLanguageAndRegion(): LanguageAndRegion {
    val subtags: List<String> = replace(
        oldChar = IDENTIFIER_SEPARATOR,
        newChar = TAG_SEPARATOR
    ).split(TAG_SEPARATOR)
    val region: String? = subtags
        .drop(n = 1)
        .takeWhile { subtag: String -> subtag.length > EXTENSION_SINGLETON_LENGTH }
        .firstOrNull { subtag: String -> subtag.isRegion() }
    return LanguageAndRegion(
        language = subtags.first().lowercase(),
        region = region?.uppercase()
    )
}

private fun String.isRegion(): Boolean =
    (length == LETTER_REGION_LENGTH && all { character: Char -> character.isLetter() }) ||
        (length == NUMBER_REGION_LENGTH && all { character: Char -> character.isDigit() })

private const val TAG_SEPARATOR = '-'
private const val IDENTIFIER_SEPARATOR = '_'
private const val EXTENSION_SINGLETON_LENGTH = 1
private const val LETTER_REGION_LENGTH = 2
private const val NUMBER_REGION_LENGTH = 3