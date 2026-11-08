package se.vadretapp.vadret.ui.extension

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.aggregatedfeed.model.SlipperinessRisk
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.slipperiness_high
import se.vadretapp.vadret.ui.resources.slipperiness_possible

fun SlipperinessRisk.labelResource(): StringResource =
    when (this) {
        SlipperinessRisk.POSSIBLE -> Res.string.slipperiness_possible
        SlipperinessRisk.HIGH -> Res.string.slipperiness_high
    }