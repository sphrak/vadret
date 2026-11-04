package se.vadretapp.vadret.weatherforecast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.cancel
import se.vadretapp.vadret.ui.resources.place_label
import se.vadretapp.vadret.ui.resources.place_label_missing_text
import se.vadretapp.vadret.ui.resources.place_label_missing_title
import se.vadretapp.vadret.ui.resources.place_name_taken_text
import se.vadretapp.vadret.ui.resources.save
import se.vadretapp.vadret.weatherforecast.model.PlaceNaming

@Composable
internal fun PlaceLabelDialog(
    placeNaming: PlaceNaming,
    onLabelChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val takenPlaceName: String? = placeNaming.takenPlaceName
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(resource = Res.string.place_label_missing_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.medium)) {
                Text(
                    text = if (takenPlaceName != null) {
                        stringResource(
                            resource = Res.string.place_name_taken_text,
                            takenPlaceName
                        )
                    } else {
                        stringResource(resource = Res.string.place_label_missing_text)
                    }
                )
                TextField(
                    value = placeNaming.label,
                    onValueChange = onLabelChanged,
                    placeholder = { Text(text = stringResource(resource = Res.string.place_label)) },
                    isError = placeNaming.isLabelMissing,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSave() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(text = stringResource(resource = Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(resource = Res.string.cancel))
            }
        },
        modifier = modifier
    )
}