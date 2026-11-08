package se.vadretapp.vadret.settings.extension

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.setting.model.HourFormatChoice
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.hour_format_12
import se.vadretapp.vadret.ui.resources.hour_format_24
import se.vadretapp.vadret.ui.resources.system_hour_format

internal fun HourFormatChoice.labelResource(): StringResource =
    when (this) {
        HourFormatChoice.SYSTEM -> Res.string.system_hour_format
        HourFormatChoice.TWENTY_FOUR_HOUR -> Res.string.hour_format_24
        HourFormatChoice.TWELVE_HOUR -> Res.string.hour_format_12
    }