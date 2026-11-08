package se.vadretapp.vadret.ui.extension

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.aggregatedfeed.model.SlipperinessCause
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.slipperiness_cause_freezing_rain
import se.vadretapp.vadret.ui.resources.slipperiness_cause_freezing_rain_explanation
import se.vadretapp.vadret.ui.resources.slipperiness_cause_frost
import se.vadretapp.vadret.ui.resources.slipperiness_cause_frost_explanation
import se.vadretapp.vadret.ui.resources.slipperiness_cause_refreeze
import se.vadretapp.vadret.ui.resources.slipperiness_cause_refreeze_explanation
import se.vadretapp.vadret.ui.resources.slipperiness_cause_snow
import se.vadretapp.vadret.ui.resources.slipperiness_cause_snow_explanation

fun SlipperinessCause.nameResource(): StringResource =
    when (this) {
        SlipperinessCause.FROST -> Res.string.slipperiness_cause_frost
        SlipperinessCause.FREEZING_RAIN -> Res.string.slipperiness_cause_freezing_rain
        SlipperinessCause.REFREEZE -> Res.string.slipperiness_cause_refreeze
        SlipperinessCause.SNOW -> Res.string.slipperiness_cause_snow
    }

fun SlipperinessCause.explanationResource(): StringResource =
    when (this) {
        SlipperinessCause.FROST -> Res.string.slipperiness_cause_frost_explanation
        SlipperinessCause.FREEZING_RAIN -> Res.string.slipperiness_cause_freezing_rain_explanation
        SlipperinessCause.REFREEZE -> Res.string.slipperiness_cause_refreeze_explanation
        SlipperinessCause.SNOW -> Res.string.slipperiness_cause_snow_explanation
    }