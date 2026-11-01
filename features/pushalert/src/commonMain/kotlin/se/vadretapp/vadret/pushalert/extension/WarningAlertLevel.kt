package se.vadretapp.vadret.pushalert.extension

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.warning_level_message
import se.vadretapp.vadret.core.resources.warning_level_orange
import se.vadretapp.vadret.core.resources.warning_level_red
import se.vadretapp.vadret.core.resources.warning_level_yellow
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel

internal fun WarningAlertLevel.labelResource(): StringResource =
    when (this) {
        WarningAlertLevel.MESSAGE -> Res.string.warning_level_message
        WarningAlertLevel.YELLOW -> Res.string.warning_level_yellow
        WarningAlertLevel.ORANGE -> Res.string.warning_level_orange
        WarningAlertLevel.RED -> Res.string.warning_level_red
    }