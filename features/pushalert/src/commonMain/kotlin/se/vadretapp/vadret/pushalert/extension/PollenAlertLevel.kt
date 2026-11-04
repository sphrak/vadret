package se.vadretapp.vadret.pushalert.extension

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.pollen_level_high
import se.vadretapp.vadret.ui.resources.pollen_level_high_to_very_high
import se.vadretapp.vadret.ui.resources.pollen_level_low
import se.vadretapp.vadret.ui.resources.pollen_level_low_to_moderate
import se.vadretapp.vadret.ui.resources.pollen_level_moderate
import se.vadretapp.vadret.ui.resources.pollen_level_moderate_to_high
import se.vadretapp.vadret.ui.resources.pollen_level_very_high

internal fun PollenAlertLevel.labelResource(): StringResource =
    when (this) {
        PollenAlertLevel.LOW -> Res.string.pollen_level_low
        PollenAlertLevel.LOW_TO_MODERATE -> Res.string.pollen_level_low_to_moderate
        PollenAlertLevel.MODERATE -> Res.string.pollen_level_moderate
        PollenAlertLevel.MODERATE_TO_HIGH -> Res.string.pollen_level_moderate_to_high
        PollenAlertLevel.HIGH -> Res.string.pollen_level_high
        PollenAlertLevel.HIGH_TO_VERY_HIGH -> Res.string.pollen_level_high_to_very_high
        PollenAlertLevel.VERY_HIGH -> Res.string.pollen_level_very_high
    }