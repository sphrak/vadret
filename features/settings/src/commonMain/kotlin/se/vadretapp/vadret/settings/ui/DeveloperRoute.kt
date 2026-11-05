package se.vadretapp.vadret.settings.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.settings.DeveloperEvent
import se.vadretapp.vadret.settings.DeveloperState
import se.vadretapp.vadret.settings.DeveloperViewModel
import se.vadretapp.vadret.settings.Settings
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.SettingsScaffold
import se.vadretapp.vadret.ui.SettingsSectionHeader
import se.vadretapp.vadret.ui.SwitchRow
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.allow_expand_once
import se.vadretapp.vadret.ui.resources.developer_settings_title
import se.vadretapp.vadret.ui.resources.end_of_season_is_not_simulated
import se.vadretapp.vadret.ui.resources.end_of_season_is_simulated
import se.vadretapp.vadret.ui.resources.expand_once_is_disabled
import se.vadretapp.vadret.ui.resources.expand_once_is_enabled
import se.vadretapp.vadret.ui.resources.pollen
import se.vadretapp.vadret.ui.resources.simulate_end_of_season
import se.vadretapp.vadret.ui.resources.weather

private const val WEATHER_HEADER_KEY = "weather-header"
private const val EXPAND_ONCE_KEY = "expand-once"
private const val POLLEN_HEADER_KEY = "pollen-header"
private const val END_OF_SEASON_KEY = "end-of-season"

private enum class DeveloperContentType {
    HEADER,
    ROW
}

@Composable
fun DeveloperRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeveloperViewModel = koinViewModel()
) {
    val state: DeveloperState by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.onEvent(event = DeveloperEvent.OnStart)
    }
    DeveloperScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun DeveloperScreen(
    state: DeveloperState,
    onEvent: (DeveloperEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings: Settings? = state.settings
    SettingsScaffold(
        title = stringResource(resource = Res.string.developer_settings_title),
        onBack = onBack,
        modifier = modifier
    ) {
        if (settings != null) {
            item(key = WEATHER_HEADER_KEY, contentType = DeveloperContentType.HEADER) {
                SettingsSectionHeader(text = stringResource(resource = Res.string.weather))
            }
            item(key = EXPAND_ONCE_KEY, contentType = DeveloperContentType.ROW) {
                SwitchRow(
                    title = stringResource(resource = Res.string.allow_expand_once),
                    summary = stringResource(
                        resource = summaryResource(
                            isChecked = settings.isExpandOnceEnabled,
                            checkedResource = Res.string.expand_once_is_enabled,
                            uncheckedResource = Res.string.expand_once_is_disabled
                        )
                    ),
                    isChecked = settings.isExpandOnceEnabled,
                    onCheckedChange = { isChecked: Boolean -> onEvent(DeveloperEvent.OnExpandOnceToggled(isExpandOnceEnabled = isChecked)) }
                )
            }
            item(key = POLLEN_HEADER_KEY, contentType = DeveloperContentType.HEADER) {
                SettingsSectionHeader(text = stringResource(resource = Res.string.pollen))
            }
            item(key = END_OF_SEASON_KEY, contentType = DeveloperContentType.ROW) {
                SwitchRow(
                    title = stringResource(resource = Res.string.simulate_end_of_season),
                    summary = stringResource(
                        resource = summaryResource(
                            isChecked = settings.isEndOfSeasonSimulated,
                            checkedResource = Res.string.end_of_season_is_simulated,
                            uncheckedResource = Res.string.end_of_season_is_not_simulated
                        )
                    ),
                    isChecked = settings.isEndOfSeasonSimulated,
                    onCheckedChange = { isChecked: Boolean -> onEvent(DeveloperEvent.OnEndOfSeasonSimulationToggled(isEndOfSeasonSimulated = isChecked)) }
                )
            }
        }
    }
}

private fun summaryResource(
    isChecked: Boolean,
    checkedResource: StringResource,
    uncheckedResource: StringResource
): StringResource =
    if (isChecked) {
        checkedResource
    } else {
        uncheckedResource
    }

@Preview(device = Devices.PHONE)
@Composable
private fun DeveloperScreenPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        DeveloperScreen(
            state = developerPreviewState(theme = theme),
            onEvent = {},
            onBack = {},
            modifier = Modifier.background(color = VadretTheme.colorScheme.background)
        )
    }
}