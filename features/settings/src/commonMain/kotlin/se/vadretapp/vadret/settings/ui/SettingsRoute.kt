package se.vadretapp.vadret.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.changelog.model.Changelog
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.followed_places_limit
import se.vadretapp.vadret.core.resources.followed_places_limit_text
import se.vadretapp.vadret.core.resources.followed_places_only_in_sweden
import se.vadretapp.vadret.core.resources.followed_places_only_in_sweden_text
import se.vadretapp.vadret.core.resources.ok
import se.vadretapp.vadret.core.resources.select_a_language
import se.vadretapp.vadret.core.resources.select_a_theme
import se.vadretapp.vadret.di.koinAll
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.library.model.Library
import se.vadretapp.vadret.settings.SettingsEvent
import se.vadretapp.vadret.settings.SettingsState
import se.vadretapp.vadret.settings.SettingsViewModel
import se.vadretapp.vadret.settings.extension.labelResource
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.ChoiceDialog
import se.vadretapp.vadret.ui.SettingsSection
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.VadretTopAppBar

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
    settingsSections: List<SettingsSection> = koinAll()
) {
    val state: SettingsState by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        settingsSections = settingsSections,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
internal fun SettingsScreen(
    state: SettingsState,
    settingsSections: List<SettingsSection>,
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
                    settingsSections = settingsSections,
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
    if (state.isFollowedPlaceLimitDialogVisible) {
        FollowedPlaceDialog(
            titleResource = Res.string.followed_places_limit,
            textResource = Res.string.followed_places_limit_text,
            onDismiss = { onEvent(SettingsEvent.OnFollowedPlaceLimitDialogDismissed) }
        )
    }
    if (state.isFollowedPlaceAbroadDialogVisible) {
        FollowedPlaceDialog(
            titleResource = Res.string.followed_places_only_in_sweden,
            textResource = Res.string.followed_places_only_in_sweden_text,
            onDismiss = { onEvent(SettingsEvent.OnFollowedPlaceAbroadDialogDismissed) }
        )
    }
    if (state.isPollenAllergenDialogVisible) {
        PollenAllergenDialog(
            pollenAllergenRows = state.pollenAllergenRows,
            arePollenTypesUnavailable = state.arePollenTypesUnavailable,
            onAllergenToggled = { pollenId: String, isAllergen: Boolean ->
                onEvent(
                    SettingsEvent.OnPollenAllergenToggled(
                        pollenId = pollenId,
                        isAllergen = isAllergen
                    )
                )
            },
            onDismiss = { onEvent(SettingsEvent.OnPollenAllergensDialogDismissed) }
        )
    }
}

@Composable
private fun FollowedPlaceDialog(
    titleResource: StringResource,
    textResource: StringResource,
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
        title = { Text(text = stringResource(resource = titleResource)) },
        text = { Text(text = stringResource(resource = textResource)) },
        modifier = modifier
    )
}

@Preview(device = Devices.PHONE)
@Composable
private fun SettingsScreenPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        SettingsScreen(
            state = settingsPreviewState(theme = theme),
            settingsSections = emptyList(),
            onEvent = {},
            modifier = Modifier.background(color = VadretTheme.colorScheme.background)
        )
    }
}