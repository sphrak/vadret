package fi.kroon.vadret.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val rowMinimumHeight = 52.dp
private val trailingIconSize = 20.dp

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
            start = VadretTheme.spacing.small,
            end = VadretTheme.spacing.small,
            top = VadretTheme.spacing.large,
            bottom = VadretTheme.spacing.small
        )
    )
}

@Composable
fun SettingsRowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        color = VadretTheme.colors.divider,
        modifier = modifier.padding(start = VadretTheme.spacing.large)
    )
}

@Composable
fun SettingsValueRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    trailingIconResource: DrawableResource? = null,
    onClick: (() -> Unit)? = null
) {
    val clickModifier: Modifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = rowMinimumHeight)
            .then(clickModifier)
            .padding(
                horizontal = VadretTheme.spacing.large,
                vertical = VadretTheme.spacing.medium
            )
    ) {
        Text(
            text = title,
            style = VadretTheme.textStyles.listItem,
            color = VadretTheme.colorScheme.onSurface,
            modifier = Modifier.weight(weight = 1f)
        )
        Text(
            text = value,
            style = VadretTheme.textStyles.body,
            color = VadretTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
        if (trailingIconResource != null) {
            Icon(
                painter = painterResource(resource = trailingIconResource),
                contentDescription = null,
                tint = VadretTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size = trailingIconSize)
            )
        }
    }
}