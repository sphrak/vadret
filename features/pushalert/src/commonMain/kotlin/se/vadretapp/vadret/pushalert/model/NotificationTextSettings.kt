package se.vadretapp.vadret.pushalert.model

import se.vadretapp.vadret.setting.model.AppLanguage
import se.vadretapp.vadret.setting.model.HourFormat

internal data class NotificationTextSettings(
    val language: AppLanguage,
    val hourFormat: HourFormat
)