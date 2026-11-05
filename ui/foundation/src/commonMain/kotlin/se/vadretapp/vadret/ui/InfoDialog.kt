package se.vadretapp.vadret.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.ok

@Composable
fun InfoDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(resource = Res.string.ok))
            }
        },
        title = { Text(text = title) },
        text = { Text(text = text) },
        modifier = modifier
    )
}