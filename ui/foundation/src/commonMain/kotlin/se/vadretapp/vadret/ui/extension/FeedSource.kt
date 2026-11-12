package se.vadretapp.vadret.ui.extension

import org.jetbrains.compose.resources.DrawableResource
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.ic_campaign
import se.vadretapp.vadret.ui.resources.ic_info
import se.vadretapp.vadret.ui.resources.ic_warning

private const val KRISINFORMATION_NAME = "Krisinformation"
private const val SMHI_NAME = "SMHI"
private const val VADRET_API_NAME = "Vadretapp.se"

fun FeedSource.displayName(): String =
    when (this) {
        FeedSource.KRISINFORMATION -> KRISINFORMATION_NAME
        FeedSource.SMHI -> SMHI_NAME
        FeedSource.VADRET_API -> VADRET_API_NAME
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
        FeedSource.VADRET_API -> Res.drawable.ic_warning
    }