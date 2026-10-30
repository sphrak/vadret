package fi.kroon.vadret.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.kroon.vadret.changelog.model.Changelog
import fi.kroon.vadret.core.resources.Res
import fi.kroon.vadret.core.resources.select_a_language
import fi.kroon.vadret.core.resources.select_a_theme
import fi.kroon.vadret.di.koinViewModel
import fi.kroon.vadret.language.model.AppLanguage
import fi.kroon.vadret.library.model.Library
import fi.kroon.vadret.settings.SettingsEvent
import fi.kroon.vadret.settings.SettingsState
import fi.kroon.vadret.settings.SettingsViewModel
import fi.kroon.vadret.settings.extension.labelResource
import fi.kroon.vadret.theme.model.Theme
import fi.kroon.vadret.ui.ThemePreviewParameterProvider
import fi.kroon.vadret.ui.VadretTheme
import fi.kroon.vadret.ui.VadretTopAppBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state: SettingsState by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
internal fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState: PagerState = rememberPagerState(pageCount = { state.pages.size })
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        onEvent(SettingsEvent.OnStart)
    }

    Column(modifier = modifier.fillMaxSize()) {
        VadretTopAppBar()
        SettingsTabs(
            pages = state.pages,
            selectedPage = state.pages[pagerState.targetPage],
            onPageClick = { page: SettingsPage ->
                coroutineScope.launch { pagerState.animateScrollToPage(page = state.pages.indexOf(element = page)) }
            },
            modifier = Modifier.padding(horizontal = VadretTheme.spacing.large)
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex: Int ->
            when (state.pages[pageIndex]) {
                SettingsPage.PREFERENCES -> PreferencesPage(
                    state = state,
                    onEvent = onEvent
                )
                SettingsPage.LIBRARIES -> LibrariesPage(
                    libraryGroups = state.libraryGroups,
                    onLibraryClick = { library: Library ->
                        onEvent(SettingsEvent.OnLibraryClicked(library = library))
                    }
                )
                SettingsPage.ABOUT -> AboutPage(
                    aboutRows = state.aboutRows,
                    onChangelogClick = { onEvent(SettingsEvent.OnChangelogClicked) }
                )
                SettingsPage.DEVELOPER -> DeveloperPage(
                    state = state,
                    onEvent = onEvent
                )
            }
        }
    }

    val selectedLibrary: Library? = state.selectedLibrary
    if (selectedLibrary != null) {
        LibrarySheet(
            library = selectedLibrary,
            onDismiss = { onEvent(SettingsEvent.OnLibraryDismissed) }
        )
    }

    val changelog: Changelog? = state.changelog
    if (state.isChangelogVisible && changelog != null) {
        ChangelogSheet(
            changelog = changelog,
            onDismiss = { onEvent(SettingsEvent.OnChangelogDismissed) }
        )
    }

    val settings = state.settings
    if (state.isThemeDialogVisible && settings != null) {
        ChoiceDialog(
            title = stringResource(resource = Res.string.select_a_theme),
            choices = Theme.entries,
            selectedChoice = settings.theme,
            choiceLabel = { theme: Theme -> stringResource(resource = theme.labelResource()) },
            onChoiceSelected = { theme: Theme -> onEvent(SettingsEvent.OnThemeSelected(theme = theme)) },
            onDismiss = { onEvent(SettingsEvent.OnThemeDialogDismissed) }
        )
    }
    if (state.isLanguageDialogVisible && settings != null) {
        ChoiceDialog(
            title = stringResource(resource = Res.string.select_a_language),
            choices = AppLanguage.entries,
            selectedChoice = settings.language,
            choiceLabel = { language: AppLanguage -> stringResource(resource = language.labelResource()) },
            onChoiceSelected = { language: AppLanguage -> onEvent(SettingsEvent.OnLanguageSelected(language = language)) },
            onDismiss = { onEvent(SettingsEvent.OnLanguageDialogDismissed) }
        )
    }
}

@Preview(device = Devices.PHONE)
@Composable
private fun SettingsScreenPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        SettingsScreen(
            state = settingsPreviewState(theme = theme),
            onEvent = {},
            modifier = Modifier.background(color = VadretTheme.colorScheme.background)
        )
    }
}