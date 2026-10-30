package se.vadretapp.vadret.settings.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import se.vadretapp.vadret.ui.VadretTheme

@Composable
internal fun CheckboxRow(
    text: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = isChecked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
            .padding(vertical = VadretTheme.spacing.small)
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = null
        )
        Text(
            text = text,
            style = VadretTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = VadretTheme.spacing.medium)
        )
    }
}