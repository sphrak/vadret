package fi.kroon.vadret.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import fi.kroon.vadret.aggregatedfeed.model.WarningLevel

@Immutable
data class WarningColors(
    val container: Color,
    val content: Color
)

@Composable
@ReadOnlyComposable
fun warningColors(
    warningLevel: WarningLevel?,
    isPublicAlert: Boolean
): WarningColors =
    when (
        if (isPublicAlert) {
            WarningLevel.RED
        } else {
            warningLevel
        }
    ) {
        WarningLevel.YELLOW -> WarningColors(
            container = VadretTheme.colors.warningYellow,
            content = VadretTheme.colors.onWarningYellow
        )
        WarningLevel.ORANGE -> WarningColors(
            container = VadretTheme.colors.warningOrange,
            content = VadretTheme.colors.onWarningOrange
        )
        WarningLevel.RED -> WarningColors(
            container = VadretTheme.colors.warningRed,
            content = VadretTheme.colors.onWarningRed
        )
        WarningLevel.MESSAGE,
        null -> WarningColors(
            container = VadretTheme.colors.warningMessage,
            content = VadretTheme.colors.onWarningMessage
        )
    }