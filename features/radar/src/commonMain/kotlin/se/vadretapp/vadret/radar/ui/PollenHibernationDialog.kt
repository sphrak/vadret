package se.vadretapp.vadret.radar.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.close
import se.vadretapp.vadret.ui.resources.pollen_hibernating_text
import se.vadretapp.vadret.ui.resources.pollen_hibernating_title

@Composable
internal fun PollenHibernationDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(resource = Res.string.pollen_hibernating_title)) },
        text = { Text(text = stringResource(resource = Res.string.pollen_hibernating_text)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(resource = Res.string.close))
            }
        },
        modifier = modifier
    )
}