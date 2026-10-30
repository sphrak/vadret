package se.vadretapp.vadret.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.close
import se.vadretapp.vadret.core.resources.pollen_allergies
import se.vadretapp.vadret.core.resources.pollen_allergies_description
import se.vadretapp.vadret.core.resources.pollen_types_not_available
import se.vadretapp.vadret.settings.model.PollenAllergenRow
import se.vadretapp.vadret.ui.SettingsRowDivider
import se.vadretapp.vadret.ui.VadretTheme

@Composable
internal fun PollenAllergenDialog(
    pollenAllergenRows: ImmutableList<PollenAllergenRow>,
    arePollenTypesUnavailable: Boolean,
    onAllergenToggled: (pollenId: String, isAllergen: Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(resource = Res.string.pollen_allergies)) },
        text = {
            Column(modifier = Modifier.verticalScroll(state = rememberScrollState())) {
                Text(
                    text = stringResource(resource = Res.string.pollen_allergies_description),
                    style = VadretTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = VadretTheme.spacing.small)
                )
                SettingsRowDivider()
                if (arePollenTypesUnavailable) {
                    Text(
                        text = stringResource(resource = Res.string.pollen_types_not_available),
                        style = VadretTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = VadretTheme.spacing.small)
                    )
                }
                pollenAllergenRows.forEach { pollenAllergenRow: PollenAllergenRow ->
                    CheckboxRow(
                        text = pollenAllergenRow.name,
                        isChecked = pollenAllergenRow.isAllergen,
                        onCheckedChange = { isAllergen: Boolean ->
                            onAllergenToggled(
                                pollenAllergenRow.pollenId,
                                isAllergen
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(resource = Res.string.close))
            }
        },
        modifier = modifier
    )
}