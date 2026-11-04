package se.vadretapp.vadret.ui.extension

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.warning_level_message
import se.vadretapp.vadret.ui.resources.warning_level_orange
import se.vadretapp.vadret.ui.resources.warning_level_red
import se.vadretapp.vadret.ui.resources.warning_level_yellow

fun WarningLevel.labelResource(): StringResource =
    when (this) {
        WarningLevel.MESSAGE -> Res.string.warning_level_message
        WarningLevel.YELLOW -> Res.string.warning_level_yellow
        WarningLevel.ORANGE -> Res.string.warning_level_orange
        WarningLevel.RED -> Res.string.warning_level_red
    }