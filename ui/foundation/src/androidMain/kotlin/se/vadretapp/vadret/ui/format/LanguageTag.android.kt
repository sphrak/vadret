package se.vadretapp.vadret.ui.format

import java.util.Locale

actual fun currentLanguageTag(): String = Locale.getDefault().toLanguageTag()