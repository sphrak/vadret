package se.vadretapp.vadret.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import se.vadretapp.vadret.ui.format.currentLanguageTag

val LocalLanguageTag: ProvidableCompositionLocal<String> = staticCompositionLocalOf { currentLanguageTag() }

@Composable
fun LanguageEnvironment(
    languageTag: String?,
    content: @Composable () -> Unit
) {
    val shownLanguageTag: String = remember(languageTag) {
        applyLanguage(languageTag = languageTag)
        currentLanguageTag()
    }
    CompositionLocalProvider(LocalLanguageTag provides shownLanguageTag) {
        key(shownLanguageTag) {
            content()
        }
    }
}

internal expect fun applyLanguage(languageTag: String?)