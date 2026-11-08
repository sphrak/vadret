package se.vadretapp.vadret.setting.local

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Foundation.NSCurrentLocaleDidChangeNotification
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSNotificationCenter
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.Foundation.preferredLanguages
import se.vadretapp.vadret.setting.model.SystemSettingsEntity

private const val HOUR_TEMPLATE = "j"
private const val QUOTE = '\''
private val twelveHourSymbols: Set<Char> = setOf('h', 'K')
private const val IDENTIFIER_SEPARATOR = '_'
private const val TAG_SEPARATOR = '-'

internal class IosSystemSettingLocalDataSource : SystemSettingLocalDataSource {

    override fun getSystemSettings(): SystemSettingsEntity = SystemSettingsEntity(
        isTwentyFourHour = isTwentyFourHour(),
        languageTag = languageTag()
    )

    // iOS tells the app when the clock or the language of the phone is switched, also while the app runs.
    override fun observeSystemSettings(): Flow<SystemSettingsEntity> =
        callbackFlow {
            val observer = NSNotificationCenter.defaultCenter.addObserverForName(
                name = NSCurrentLocaleDidChangeNotification,
                `object` = null,
                queue = null,
                usingBlock = { _ -> trySend(element = getSystemSettings()) }
            )
            trySend(element = getSystemSettings())
            awaitClose { NSNotificationCenter.defaultCenter.removeObserver(observer = observer) }
        }.distinctUntilChanged()

    // The hour the phone would write for its own settings is on the 12 hour clock when it uses an h or K outside quotes.
    private fun isTwentyFourHour(): Boolean {
        val hourPattern: String = NSDateFormatter.dateFormatFromTemplate(
            tmplate = HOUR_TEMPLATE,
            options = 0u,
            locale = NSLocale.currentLocale
        ) ?: return true
        return hourPattern
            .split(QUOTE)
            .filterIndexed { index: Int, _: String -> index % 2 == 0 }
            .none { unquoted: String -> unquoted.any { symbol: Char -> symbol in twelveHourSymbols } }
    }

    private fun languageTag(): String {
        val preferredLanguage: String? = NSLocale.preferredLanguages.firstOrNull() as? String
        val identifier: String = preferredLanguage ?: NSLocale.currentLocale.localeIdentifier
        return identifier.replace(
            oldChar = IDENTIFIER_SEPARATOR,
            newChar = TAG_SEPARATOR
        )
    }
}