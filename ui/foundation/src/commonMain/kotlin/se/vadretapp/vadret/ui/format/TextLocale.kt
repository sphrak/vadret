package se.vadretapp.vadret.ui.format

import se.vadretapp.vadret.setting.model.HourFormat

data class TextLocale(
    val languageTag: String,
    val hourFormat: HourFormat
)