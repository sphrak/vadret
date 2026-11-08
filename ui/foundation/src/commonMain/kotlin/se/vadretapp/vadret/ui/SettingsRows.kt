package se.vadretapp.vadret.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.ic_info

@Composable
fun SettingsSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = VadretTheme.textStyles.detailStrong,
        color = VadretTheme.colorScheme.primary,
        modifier = modifier.padding(
            start = VadretTheme.spacing.large,
            end = VadretTheme.spacing.large,
            top = VadretTheme.spacing.large,
            bottom = VadretTheme.spacing.small
        )
    )
}

@Composable
fun SettingsRowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = VadretTheme.colors.divider,
        modifier = modifier
    )
}

// Android's own settings give every row at least this height and this much room above and below the text, so the rows
// here sit like the phone's settings do.
private val settingsRowMinimumHeight: Dp = 72.dp
private val settingsRowVerticalPadding: Dp = 16.dp
private val settingsRowHorizontalPadding: Dp = 16.dp

internal val settingsRowContentPadding: PaddingValues = PaddingValues(
    horizontal = settingsRowHorizontalPadding,
    vertical = settingsRowVerticalPadding
)

internal fun Modifier.settingsRowHeight(): Modifier = heightIn(min = settingsRowMinimumHeight)

@Composable
fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    iconResource: DrawableResource? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    SettingsRow(
        title = title,
        onClick = onClick,
        leadingContent = rowLeadingContent(iconResource = iconResource),
        modifier = modifier,
        summary = summary,
        trailingContent = trailingContent
    )
}

// For a row whose leading icon is not a drawable, such as SettingsRowIcon, or that only keeps an icon's room with
// SettingsRowIconSpace.
@Composable
fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    leadingContent: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    summary: String? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    ListItem(
        onClick = onClick,
        modifier = modifier.settingsRowHeight(),
        leadingContent = leadingContent,
        trailingContent = if (trailingContent != null) {
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    content = trailingContent
                )
            }
        } else {
            null
        },
        supportingContent = rowSupportingContent(summary = summary),
        shapes = settingsRowShapes(),
        colors = settingsRowColors(),
        contentPadding = settingsRowContentPadding
    ) {
        Text(text = title)
    }
}

internal fun rowLeadingContent(iconResource: DrawableResource?): (@Composable () -> Unit)? =
    if (iconResource != null) {
        {
            Icon(
                painter = painterResource(resource = iconResource),
                contentDescription = null,
                modifier = Modifier.settingsRowIconPadding()
            )
        }
    } else {
        null
    }

internal fun rowSupportingContent(summary: String?): (@Composable () -> Unit)? =
    if (summary != null) {
        { Text(text = summary) }
    } else {
        null
    }

// The screen colour shows through, so rows sit on the background like the rest of the screen. A row that is on looks
// like any other row, since its switch already shows that it is on.
@Composable
internal fun settingsRowColors(): ListItemColors {
    val defaultColors: ListItemColors = ListItemDefaults.colors()
    return ListItemDefaults.colors(
        containerColor = Color.Transparent,
        selectedContainerColor = Color.Transparent,
        selectedContentColor = defaultColors.contentColor,
        selectedLeadingContentColor = defaultColors.leadingContentColor,
        selectedTrailingContentColor = defaultColors.trailingContentColor,
        selectedOverlineContentColor = defaultColors.overlineContentColor,
        selectedSupportingContentColor = defaultColors.supportingContentColor
    )
}

// Rows span the screen edge to edge, so a press fills the whole row instead of rounding into a card.
@Composable
internal fun settingsRowShapes(): ListItemShapes =
    ListItemDefaults.shapes(
        shape = RectangleShape,
        selectedShape = RectangleShape,
        pressedShape = RectangleShape,
        focusedShape = RectangleShape,
        hoveredShape = RectangleShape,
        draggedShape = RectangleShape
    )

@Composable
fun SettingsNote(
    text: String,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier.settingsRowHeight(),
        leadingContent = {
            Icon(
                painter = painterResource(resource = Res.drawable.ic_info),
                contentDescription = null,
                modifier = Modifier.settingsRowIconPadding()
            )
        },
        colors = settingsRowColors(),
        contentPadding = settingsRowContentPadding
    ) {
        Text(text = text)
    }
}

@Composable
fun SettingsFootnote(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = VadretTheme.textStyles.detail,
        color = VadretTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(
            start = VadretTheme.spacing.large,
            end = VadretTheme.spacing.large,
            top = VadretTheme.spacing.small,
            bottom = VadretTheme.spacing.large
        )
    )
}