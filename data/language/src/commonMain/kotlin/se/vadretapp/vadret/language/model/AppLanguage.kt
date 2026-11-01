package se.vadretapp.vadret.language.model

enum class AppLanguage(val languageTag: String?) {
    SYSTEM(languageTag = null),
    ENGLISH(languageTag = "en"),
    SWEDISH(languageTag = "sv-SE"),
    DANISH(languageTag = "da-DK"),
    GERMAN(languageTag = "de-DE"),
    FINNISH(languageTag = "fi-FI"),
    FRENCH(languageTag = "fr-FR"),
    NORWEGIAN(languageTag = "no-NO"),
    POLISH(languageTag = "pl-PL")
}