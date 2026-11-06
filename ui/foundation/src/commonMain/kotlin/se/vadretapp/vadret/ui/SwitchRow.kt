package se.vadretapp.vadret.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.ic_check

// The whole row toggles, so the switch itself only shows the state.
@Composable
fun SwitchRow(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    iconResource: DrawableResource? = null
) {
    SwitchRow(
        title = title,
        isChecked = isChecked,
        onCheckedChange = onCheckedChange,
        leadingContent = rowLeadingContent(iconResource = iconResource),
        modifier = modifier,
        summary = summary
    )
}

// For a row whose leading icon is not a drawable, such as SettingsRowIcon, or that only keeps an icon's room with
// SettingsRowIconSpace.
@Composable
fun SwitchRow(
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leadingContent: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    summary: String? = null
) {
    ListItem(
        checked = isChecked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.settingsRowHeight(),
        leadingContent = leadingContent,
        trailingContent = {
            Switch(
                checked = isChecked,
                onCheckedChange = null,
                thumbContent = if (isChecked) {
                    {
                        Icon(
                            painter = painterResource(resource = Res.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(size = SwitchDefaults.IconSize)
                        )
                    }
                } else {
                    null
                },
                colors = SwitchDefaults.colors(
                    uncheckedThumbColor = VadretTheme.colorScheme.onSurfaceVariant,
                    uncheckedBorderColor = VadretTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = VadretTheme.colorScheme.surfaceContainerHighest
                )
            )
        },
        supportingContent = rowSupportingContent(summary = summary),
        shapes = settingsRowShapes(),
        colors = settingsRowColors(),
        contentPadding = settingsRowContentPadding
    ) {
        Text(text = title)
    }
}