package se.vadretapp.vadret.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.SlipperinessRisk
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel
import se.vadretapp.vadret.ui.extension.displayName
import se.vadretapp.vadret.ui.extension.labelResource

private const val PUBLIC_ALERT_LABEL = "VMA"

@Composable
fun warningLabel(
    source: FeedSource,
    warningLevel: WarningLevel?,
    isPublicAlert: Boolean,
    slipperinessRisk: SlipperinessRisk?
): String =
    when {
        isPublicAlert -> PUBLIC_ALERT_LABEL
        slipperinessRisk != null -> stringResource(resource = slipperinessRisk.labelResource())
        warningLevel != null -> stringResource(resource = warningLevel.labelResource())
        else -> source.displayName()
    }

@Composable
fun warningTag(
    warningLevel: WarningLevel?,
    isPublicAlert: Boolean,
    slipperinessRisk: SlipperinessRisk?
): String? =
    when {
        isPublicAlert -> PUBLIC_ALERT_LABEL
        slipperinessRisk != null -> stringResource(resource = slipperinessRisk.labelResource())
        warningLevel == WarningLevel.YELLOW ||
            warningLevel == WarningLevel.ORANGE ||
            warningLevel == WarningLevel.RED -> stringResource(resource = warningLevel.labelResource())
        else -> null
    }