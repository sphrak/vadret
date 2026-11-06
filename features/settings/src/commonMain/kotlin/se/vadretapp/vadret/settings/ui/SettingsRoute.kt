package se.vadretapp.vadret.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
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
import se.vadretapp.vadret.di.koinAll
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.icons.Pollen
import se.vadretapp.vadret.icons.VadretIcons
import se.vadretapp.vadret.language.model.AppLanguage
import se.vadretapp.vadret.settings.Settings
import se.vadretapp.vadret.settings.SettingsEvent
import se.vadretapp.vadret.settings.SettingsState
import se.vadretapp.vadret.settings.SettingsViewModel
import se.vadretapp.vadret.settings.extension.labelResource
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.ChoiceDialog
import se.vadretapp.vadret.ui.SettingsRow
import se.vadretapp.vadret.ui.SettingsRowDivider
import se.vadretapp.vadret.ui.SettingsRowIcon
import se.vadretapp.vadret.ui.SettingsSection
import se.vadretapp.vadret.ui.SwitchRow
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.VadretTopAppBar
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.developer_settings_title
import se.vadretapp.vadret.ui.resources.ic_code
import se.vadretapp.vadret.ui.resources.ic_dark_mode
import se.vadretapp.vadret.ui.resources.ic_info
import se.vadretapp.vadret.ui.resources.ic_language
import se.vadretapp.vadret.ui.resources.ic_location_on
import se.vadretapp.vadret.ui.resources.ic_schedule
import se.vadretapp.vadret.ui.resources.pollen
import se.vadretapp.vadret.ui.resources.select_a_language
import se.vadretapp.vadret.ui.resources.select_a_theme
import se.vadretapp.vadret.ui.resources.settings_about
import se.vadretapp.vadret.ui.resources.settings_hourly_forecast
import se.vadretapp.vadret.ui.resources.settings_hourly_forecast_summary
import se.vadretapp.vadret.ui.resources.settings_language
import se.vadretapp.vadret.ui.resources.settings_places
import se.vadretapp.vadret.ui.resources.theme

private const val PLACES_KEY = "places"
private const val POLLEN_KEY = "pollen"
private const val DISPLAY_DIVIDER_KEY = "display-divider"
private const val THEME_KEY = "theme"
private const val LANGUAGE_KEY = "language"
private const val HOURLY_FORECAST_KEY = "hourly-forecast"
private const val ABOUT_DIVIDER_KEY = "about-divider"
private const val DEVELOPER_KEY = "developer"
private const val ABOUT_KEY = "about"

private enum class SettingsContentType {
    ROW,
    DIVIDER
}

@Composable
fun SettingsRoute(
    onPlacesClick: () -> Unit,
    onSectionClick: (sectionKey: String) -> Unit,
    onPollenClick: () -> Unit,
    onDeveloperClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
    settingsSections: List<SettingsSection> = koinAll()
) {
    val state: SettingsState by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.onEvent(event = SettingsEvent.OnStart)
    }
    SettingsScreen(
        state = state,
        settingsSections = settingsSections,
        onEvent = viewModel::onEvent,
        onPlacesClick = onPlacesClick,
        onSectionClick = onSectionClick,
        onPollenClick = onPollenClick,
        onDeveloperClick = onDeveloperClick,
        onAboutClick = onAboutClick,
        modifier = modifier
    )
}

@Composable
private fun SettingsScreen(
    state: SettingsState,
    settingsSections: List<SettingsSection>,
    onEvent: (SettingsEvent) -> Unit,
    onPlacesClick: () -> Unit,
    onSectionClick: (sectionKey: String) -> Unit,
    onPollenClick: () -> Unit,
    onDeveloperClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        VadretTopAppBar()
        SettingsList(
            state = state,
            settingsSections = settingsSections,
            onEvent = onEvent,
            onPlacesClick = onPlacesClick,
            onSectionClick = onSectionClick,
            onPollenClick = onPollenClick,
            onDeveloperClick = onDeveloperClick,
            onAboutClick = onAboutClick,
            modifier = Modifier.fillMaxSize()
        )
    }
    SettingsDialogs(
        state = state,
        onEvent = onEvent
    )
}

@Composable
private fun SettingsList(
    state: SettingsState,
    settingsSections: List<SettingsSection>,
    onEvent: (SettingsEvent) -> Unit,
    onPlacesClick: () -> Unit,
    onSectionClick: (sectionKey: String) -> Unit,
    onPollenClick: () -> Unit,
    onDeveloperClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings: Settings? = state.settings
    LazyColumn(modifier = modifier) {
        item(key = PLACES_KEY, contentType = SettingsContentType.ROW) {
            SettingsRow(
                title = stringResource(resource = Res.string.settings_places),
                summary = state.placesSummary?.text(),
                iconResource = Res.drawable.ic_location_on,
                onClick = onPlacesClick
            )
        }
        items(
            items = settingsSections,
            key = { settingsSection: SettingsSection -> settingsSection.key },
            contentType = { SettingsContentType.ROW }
        ) { settingsSection: SettingsSection ->
            SettingsRow(
                title = stringResource(resource = settingsSection.titleResource),
                iconResource = settingsSection.iconResource,
                onClick = { onSectionClick(settingsSection.key) }
            )
        }
        item(key = POLLEN_KEY, contentType = SettingsContentType.ROW) {
            SettingsRow(
                title = stringResource(resource = Res.string.pollen),
                onClick = onPollenClick,
                leadingContent = { SettingsRowIcon(icon = VadretIcons.Alert.Pollen) }
            )
        }
        if (settings != null) {
            item(key = DISPLAY_DIVIDER_KEY, contentType = SettingsContentType.DIVIDER) {
                SettingsRowDivider()
            }
            item(key = THEME_KEY, contentType = SettingsContentType.ROW) {
                SettingsRow(
                    title = stringResource(resource = Res.string.theme),
                    summary = stringResource(resource = settings.theme.labelResource()),
                    iconResource = Res.drawable.ic_dark_mode,
                    onClick = { onEvent(SettingsEvent.OnThemeRowClicked) }
                )
            }
            item(key = LANGUAGE_KEY, contentType = SettingsContentType.ROW) {
                SettingsRow(
                    title = stringResource(resource = Res.string.settings_language),
                    summary = stringResource(resource = settings.language.labelResource()),
                    iconResource = Res.drawable.ic_language,
                    onClick = { onEvent(SettingsEvent.OnLanguageRowClicked) }
                )
            }
            item(key = HOURLY_FORECAST_KEY, contentType = SettingsContentType.ROW) {
                SwitchRow(
                    title = stringResource(resource = Res.string.settings_hourly_forecast),
                    summary = stringResource(resource = Res.string.settings_hourly_forecast_summary),
                    iconResource = Res.drawable.ic_schedule,
                    isChecked = settings.isHourlyForecastEnabled,
                    onCheckedChange = { isChecked: Boolean ->
                        onEvent(SettingsEvent.OnHourlyForecastToggled(isHourlyForecastEnabled = isChecked))
                    }
                )
            }
        }
        item(key = ABOUT_DIVIDER_KEY, contentType = SettingsContentType.DIVIDER) {
            SettingsRowDivider()
        }
        if (state.isDeveloperRowVisible) {
            item(key = DEVELOPER_KEY, contentType = SettingsContentType.ROW) {
                SettingsRow(
                    title = stringResource(resource = Res.string.developer_settings_title),
                    iconResource = Res.drawable.ic_code,
                    onClick = onDeveloperClick
                )
            }
        }
        item(key = ABOUT_KEY, contentType = SettingsContentType.ROW) {
            SettingsRow(
                title = stringResource(resource = Res.string.settings_about),
                iconResource = Res.drawable.ic_info,
                onClick = onAboutClick
            )
        }
    }
}

@Composable
private fun SettingsDialogs(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings: Settings = state.settings ?: return
    if (state.isThemeDialogVisible) {
        ChoiceDialog(
            title = stringResource(resource = Res.string.select_a_theme),
            choices = Theme.entries,
            selectedChoice = settings.theme,
            choiceLabel = { theme: Theme -> stringResource(resource = theme.labelResource()) },
            onChoiceSelected = { theme: Theme -> onEvent(SettingsEvent.OnThemeSelected(theme = theme)) },
            onDismiss = { onEvent(SettingsEvent.OnThemeDialogDismissed) },
            modifier = modifier
        )
    }
    if (state.isLanguageDialogVisible) {
        ChoiceDialog(
            title = stringResource(resource = Res.string.select_a_language),
            choices = AppLanguage.entries,
            selectedChoice = settings.language,
            choiceLabel = { language: AppLanguage -> stringResource(resource = language.labelResource()) },
            onChoiceSelected = { language: AppLanguage -> onEvent(SettingsEvent.OnLanguageSelected(language = language)) },
            onDismiss = { onEvent(SettingsEvent.OnLanguageDialogDismissed) },
            modifier = modifier
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
            settingsSections = emptyList(),
            onEvent = {},
            onPlacesClick = {},
            onSectionClick = {},
            onPollenClick = {},
            onDeveloperClick = {},
            onAboutClick = {},
            modifier = Modifier.background(color = VadretTheme.colorScheme.background)
        )
    }
}