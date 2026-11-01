package fi.kroon.vadret.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import fi.kroon.vadret.core.resources.Res
import fi.kroon.vadret.core.resources.cancel
import org.jetbrains.compose.resources.stringResource

@Composable
fun <T> ChoiceDialog(
    title: String,
    choices: List<T>,
    selectedChoice: T,
    choiceLabel: @Composable (T) -> String,
    onChoiceSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(modifier = Modifier.verticalScroll(state = rememberScrollState())) {
                choices.forEach { choice: T ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = choice == selectedChoice,
                                role = Role.RadioButton,
                                onClick = { onChoiceSelected(choice) }
                            )
                            .padding(vertical = VadretTheme.spacing.small)
                    ) {
                        RadioButton(
                            selected = choice == selectedChoice,
                            onClick = null
                        )
                        Text(
                            text = choiceLabel(choice),
                            style = VadretTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = VadretTheme.spacing.medium)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(resource = Res.string.cancel))
            }
        },
        modifier = modifier
    )
}