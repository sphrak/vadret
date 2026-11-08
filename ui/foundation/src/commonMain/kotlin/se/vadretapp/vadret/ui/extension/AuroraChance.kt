package se.vadretapp.vadret.ui.extension

import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.aggregatedfeed.model.AuroraChance
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.aurora_high
import se.vadretapp.vadret.ui.resources.aurora_possible

fun AuroraChance.labelResource(): StringResource =
    when (this) {
        AuroraChance.POSSIBLE -> Res.string.aurora_possible
        AuroraChance.HIGH -> Res.string.aurora_high
    }