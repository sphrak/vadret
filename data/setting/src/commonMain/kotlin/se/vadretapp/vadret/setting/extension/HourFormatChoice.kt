package se.vadretapp.vadret.setting.extension

import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.HourFormatChoice

internal fun HourFormatChoice.toPreferenceValue(): String = name

internal fun HourFormatChoice.toHourFormat(isSystemTwentyFourHour: Boolean): HourFormat =
    when (this) {
        HourFormatChoice.SYSTEM -> if (isSystemTwentyFourHour) {
            HourFormat.TWENTY_FOUR_HOUR
        } else {
            HourFormat.TWELVE_HOUR
        }
        HourFormatChoice.TWENTY_FOUR_HOUR -> HourFormat.TWENTY_FOUR_HOUR
        HourFormatChoice.TWELVE_HOUR -> HourFormat.TWELVE_HOUR
    }