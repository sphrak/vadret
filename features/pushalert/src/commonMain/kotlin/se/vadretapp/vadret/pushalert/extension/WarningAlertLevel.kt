package se.vadretapp.vadret.pushalert.extension

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.warning_level_message
import se.vadretapp.vadret.ui.resources.warning_level_orange
import se.vadretapp.vadret.ui.resources.warning_level_red
import se.vadretapp.vadret.ui.resources.warning_level_yellow

internal fun WarningAlertLevel.labelResource(): StringResource =
    when (this) {
        WarningAlertLevel.MESSAGE -> Res.string.warning_level_message
        WarningAlertLevel.YELLOW -> Res.string.warning_level_yellow
        WarningAlertLevel.ORANGE -> Res.string.warning_level_orange
        WarningAlertLevel.RED -> Res.string.warning_level_red
    }