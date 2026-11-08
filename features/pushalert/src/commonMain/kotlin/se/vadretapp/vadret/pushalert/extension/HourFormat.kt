package se.vadretapp.vadret.pushalert.extension

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.pushalert.model.HourFormat
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.hour_format_12
import se.vadretapp.vadret.ui.resources.hour_format_24

internal fun HourFormat.labelResource(): StringResource =
    when (this) {
        HourFormat.TWENTY_FOUR_HOUR -> Res.string.hour_format_24
        HourFormat.TWELVE_HOUR -> Res.string.hour_format_12
    }