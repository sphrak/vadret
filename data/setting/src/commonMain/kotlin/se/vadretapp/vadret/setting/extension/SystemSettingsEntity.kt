package se.vadretapp.vadret.setting.extension

import se.vadretapp.vadret.setting.model.SystemSettings
import se.vadretapp.vadret.setting.model.SystemSettingsEntity

internal fun SystemSettingsEntity.toSystemSettings(): SystemSettings = SystemSettings(
    isTwentyFourHour = isTwentyFourHour,
    languageTag = languageTag
)