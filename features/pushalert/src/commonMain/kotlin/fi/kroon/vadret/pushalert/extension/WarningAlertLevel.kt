package fi.kroon.vadret.pushalert.extension

import fi.kroon.vadret.core.resources.Res
import fi.kroon.vadret.core.resources.warning_level_message
import fi.kroon.vadret.core.resources.warning_level_orange
import fi.kroon.vadret.core.resources.warning_level_red
import fi.kroon.vadret.core.resources.warning_level_yellow
import fi.kroon.vadret.pushalert.model.WarningAlertLevel
import org.jetbrains.compose.resources.StringResource

internal fun WarningAlertLevel.labelResource(): StringResource =
    when (this) {
        WarningAlertLevel.MESSAGE -> Res.string.warning_level_message
        WarningAlertLevel.YELLOW -> Res.string.warning_level_yellow
        WarningAlertLevel.ORANGE -> Res.string.warning_level_orange
        WarningAlertLevel.RED -> Res.string.warning_level_red
    }