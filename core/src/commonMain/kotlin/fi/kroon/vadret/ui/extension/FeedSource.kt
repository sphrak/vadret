package fi.kroon.vadret.ui.extension

import fi.kroon.vadret.aggregatedfeed.model.FeedSource
import fi.kroon.vadret.aggregatedfeed.model.WarningLevel
import fi.kroon.vadret.core.resources.Res
import fi.kroon.vadret.core.resources.ic_campaign
import fi.kroon.vadret.core.resources.ic_info
import fi.kroon.vadret.core.resources.ic_warning
import org.jetbrains.compose.resources.DrawableResource

private const val KRISINFORMATION_NAME = "Krisinformation"
private const val SMHI_NAME = "SMHI"

fun FeedSource.displayName(): String =
    when (this) {
        FeedSource.KRISINFORMATION -> KRISINFORMATION_NAME
        FeedSource.SMHI -> SMHI_NAME
    }

fun FeedSource.iconResource(warningLevel: WarningLevel?): DrawableResource =
    when (this) {
        FeedSource.KRISINFORMATION -> Res.drawable.ic_campaign
        FeedSource.SMHI -> when (warningLevel) {
            WarningLevel.YELLOW,
            WarningLevel.ORANGE,
            WarningLevel.RED -> Res.drawable.ic_warning
            WarningLevel.MESSAGE,
            null -> Res.drawable.ic_info
        }
    }