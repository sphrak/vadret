package se.vadretapp.vadret.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.library.model.Library
import se.vadretapp.vadret.settings.LicensesEvent
import se.vadretapp.vadret.settings.LicensesState
import se.vadretapp.vadret.settings.LicensesViewModel
import se.vadretapp.vadret.settings.model.LibraryGroup
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.SettingsFootnote
import se.vadretapp.vadret.ui.SettingsRow
import se.vadretapp.vadret.ui.SettingsScaffold
import se.vadretapp.vadret.ui.SettingsSectionHeader
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.licenses_intro
import se.vadretapp.vadret.ui.resources.licenses_title

private const val INTRO_KEY = "intro"

private enum class LicensesContentType {
    INTRO,
    LIBRARY_GROUP
}

@Composable
fun LicensesRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LicensesViewModel = koinViewModel()
) {
    val state: LicensesState by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.onEvent(event = LicensesEvent.OnStart)
    }
    LicensesScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun LicensesScreen(
    state: LicensesState,
    onEvent: (LicensesEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScaffold(
        title = stringResource(resource = Res.string.licenses_title),
        onBack = onBack,
        modifier = modifier
    ) {
        item(key = INTRO_KEY, contentType = LicensesContentType.INTRO) {
            SettingsFootnote(text = stringResource(resource = Res.string.licenses_intro))
        }
        items(
            items = state.libraryGroups,
            key = { libraryGroup: LibraryGroup -> libraryGroup.author },
            contentType = { LicensesContentType.LIBRARY_GROUP }
        ) { libraryGroup: LibraryGroup ->
            LibraryGroupSection(
                libraryGroup = libraryGroup,
                onLibraryClick = { library: Library -> onEvent(LicensesEvent.OnLibraryClicked(library = library)) }
            )
        }
    }
    val selectedLibrary: Library? = state.selectedLibrary
    if (selectedLibrary != null) {
        LibrarySheet(
            library = selectedLibrary,
            onDismiss = { onEvent(LicensesEvent.OnLibraryDismissed) }
        )
    }
}

@Composable
private fun LibraryGroupSection(
    libraryGroup: LibraryGroup,
    onLibraryClick: (Library) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SettingsSectionHeader(text = libraryGroup.author)
        libraryGroup.libraries.forEach { library: Library ->
            SettingsRow(
                title = library.title,
                summary = library.license,
                onClick = { onLibraryClick(library) }
            )
        }
    }
}

@Preview(device = Devices.PHONE)
@Composable
private fun LicensesScreenPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        LicensesScreen(
            state = licensesPreviewState(),
            onEvent = {},
            onBack = {},
            modifier = Modifier.background(color = VadretTheme.colorScheme.background)
        )
    }
}