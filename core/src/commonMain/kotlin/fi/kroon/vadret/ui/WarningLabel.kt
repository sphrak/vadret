package fi.kroon.vadret.ui

import androidx.compose.runtime.Composable
import fi.kroon.vadret.aggregatedfeed.model.FeedSource
import fi.kroon.vadret.aggregatedfeed.model.WarningLevel
import fi.kroon.vadret.ui.extension.displayName
import fi.kroon.vadret.ui.extension.labelResource
import org.jetbrains.compose.resources.stringResource

private const val PUBLIC_ALERT_LABEL = "VMA"

@Composable
fun warningLabel(
    source: FeedSource,
    warningLevel: WarningLevel?,
    isPublicAlert: Boolean
): String =
    when {
        isPublicAlert -> PUBLIC_ALERT_LABEL
        warningLevel != null -> stringResource(resource = warningLevel.labelResource())
        else -> source.displayName()
    }

@Composable
fun warningTag(
    warningLevel: WarningLevel?,
    isPublicAlert: Boolean
): String? =
    when {
        isPublicAlert -> PUBLIC_ALERT_LABEL
        warningLevel == WarningLevel.YELLOW ||
            warningLevel == WarningLevel.ORANGE ||
            warningLevel == WarningLevel.RED -> stringResource(resource = warningLevel.labelResource())
        else -> null
    }