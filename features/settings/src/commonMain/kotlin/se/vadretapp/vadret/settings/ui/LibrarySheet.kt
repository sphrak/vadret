package se.vadretapp.vadret.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.library.model.Library
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.license_third_party
import se.vadretapp.vadret.ui.resources.project_third_party
import se.vadretapp.vadret.ui.resources.source_third_party

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibrarySheet(
    library: Library,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VadretTheme.colorScheme.surface,
        modifier = modifier
    ) {
        LibraryDetails(library = library)
    }
}

@Composable
private fun LibraryDetails(
    library: Library,
    modifier: Modifier = Modifier
) {
    val uriHandler: UriHandler = LocalUriHandler.current
    val sourceUrl: String? = library.sourceUrl
    Column(
        verticalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.small),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = VadretTheme.spacing.extraLarge,
                end = VadretTheme.spacing.extraLarge,
                bottom = VadretTheme.spacing.extraLarge
            )
    ) {
        Text(
            text = library.title,
            style = VadretTheme.textStyles.title,
            color = VadretTheme.colorScheme.onSurface
        )
        Text(
            text = "${library.author}, ${library.license}",
            style = VadretTheme.textStyles.detail,
            color = VadretTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = library.description,
            style = VadretTheme.textStyles.body,
            color = VadretTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = VadretTheme.spacing.small)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.small)) {
            FilledTonalButton(onClick = { uriHandler.openUri(uri = library.projectUrl) }) {
                Text(text = stringResource(resource = Res.string.project_third_party))
            }
            FilledTonalButton(onClick = { uriHandler.openUri(uri = library.licenseUrl) }) {
                Text(text = stringResource(resource = Res.string.license_third_party))
            }
            if (sourceUrl != null) {
                FilledTonalButton(onClick = { uriHandler.openUri(uri = sourceUrl) }) {
                    Text(text = stringResource(resource = Res.string.source_third_party))
                }
            }
        }
    }
}