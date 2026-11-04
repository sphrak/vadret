package se.vadretapp.vadret.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics

private const val HIDDEN_ALPHA = 0f

@Composable
fun SwitchRow(
    title: String,
    checkedSummary: String,
    uncheckedSummary: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(all = VadretTheme.spacing.large)
    ) {
        Column(
            modifier = Modifier
                .weight(weight = 1f)
                .padding(end = VadretTheme.spacing.large)
        ) {
            Text(
                text = title,
                style = VadretTheme.textStyles.listItem,
                color = VadretTheme.colorScheme.onSurface
            )
            Box {
                SwitchSummary(
                    text = checkedSummary,
                    isShown = isChecked
                )
                SwitchSummary(
                    text = uncheckedSummary,
                    isShown = !isChecked
                )
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = VadretTheme.colorScheme.onSurfaceVariant,
                uncheckedBorderColor = VadretTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = VadretTheme.colorScheme.surfaceContainerHighest
            )
        )
    }
}

@Composable
private fun SwitchSummary(
    text: String,
    isShown: Boolean,
    modifier: Modifier = Modifier
) {
    val visibilityModifier: Modifier = if (isShown) {
        Modifier
    } else {
        Modifier
            .alpha(alpha = HIDDEN_ALPHA)
            .clearAndSetSemantics { }
    }
    Text(
        text = text,
        style = VadretTheme.textStyles.detail,
        color = VadretTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.then(other = visibilityModifier)
    )
}