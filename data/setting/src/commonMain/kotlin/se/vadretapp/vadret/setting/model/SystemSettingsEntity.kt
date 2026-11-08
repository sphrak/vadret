package se.vadretapp.vadret.setting.model

internal data class SystemSettingsEntity(
    val isTwentyFourHour: Boolean,
    val languageTag: String
)