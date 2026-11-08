package se.vadretapp.vadret.setting.local

import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.Foundation.preferredLanguages

private const val HOUR_TEMPLATE = "j"
private const val DAY_PERIOD_SYMBOL = "a"
private const val IDENTIFIER_SEPARATOR = '_'
private const val TAG_SEPARATOR = '-'

internal class IosSystemSettingLocalDataSource : SystemSettingLocalDataSource {

    // The hour the phone would write for its own settings says whether it adds a day period such as AM.
    override fun isTwentyFourHour(): Boolean =
        NSDateFormatter
            .dateFormatFromTemplate(
                tmplate = HOUR_TEMPLATE,
                options = 0u,
                locale = NSLocale.currentLocale
            )?.contains(other = DAY_PERIOD_SYMBOL) != true

    override fun languageTag(): String {
        val preferredLanguage: String? = NSLocale.preferredLanguages.firstOrNull() as? String
        val identifier: String = preferredLanguage ?: NSLocale.currentLocale.localeIdentifier
        return identifier.replace(
            oldChar = IDENTIFIER_SEPARATOR,
            newChar = TAG_SEPARATOR
        )
    }
}