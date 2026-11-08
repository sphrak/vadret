package se.vadretapp.vadret.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.format.currentLanguageTag

val LocalLanguageTag: ProvidableCompositionLocal<String> = staticCompositionLocalOf { currentLanguageTag() }

val LocalHourFormat: ProvidableCompositionLocal<HourFormat> = staticCompositionLocalOf { HourFormat.TWENTY_FOUR_HOUR }

@Composable
fun LanguageEnvironment(
    languageTag: String?,
    hourFormat: HourFormat,
    content: @Composable () -> Unit
) {
    val shownLanguageTag: String = remember(languageTag) {
        applyLanguage(languageTag = languageTag)
        currentLanguageTag()
    }
    CompositionLocalProvider(
        LocalLanguageTag provides shownLanguageTag,
        LocalHourFormat provides hourFormat
    ) {
        key(shownLanguageTag) {
            content()
        }
    }
}

expect fun applyLanguage(languageTag: String?)