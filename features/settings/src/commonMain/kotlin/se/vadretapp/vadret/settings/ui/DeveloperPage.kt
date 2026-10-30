package se.vadretapp.vadret.settings.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.allow_expand_once
import se.vadretapp.vadret.core.resources.end_of_season_is_not_simulated
import se.vadretapp.vadret.core.resources.end_of_season_is_simulated
import se.vadretapp.vadret.core.resources.expand_once_is_disabled
import se.vadretapp.vadret.core.resources.expand_once_is_enabled
import se.vadretapp.vadret.core.resources.pollen
import se.vadretapp.vadret.core.resources.simulate_end_of_season
import se.vadretapp.vadret.core.resources.weather
import se.vadretapp.vadret.settings.Settings
import se.vadretapp.vadret.settings.SettingsEvent
import se.vadretapp.vadret.settings.SettingsState
import se.vadretapp.vadret.ui.SettingsSectionHeader
import se.vadretapp.vadret.ui.SwitchRow
import se.vadretapp.vadret.ui.VadretCard
import se.vadretapp.vadret.ui.VadretTheme

private const val WEATHER_HEADER_KEY = "weather-header"
private const val EXPAND_ONCE_KEY = "expand-once"
private const val POLLEN_HEADER_KEY = "pollen-header"
private const val END_OF_SEASON_KEY = "end-of-season"

private enum class DeveloperContentType {
    HEADER,
    CARD
}

@Composable
internal fun DeveloperPage(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings: Settings = state.settings ?: return

    LazyColumn(
        contentPadding = PaddingValues(
            start = VadretTheme.spacing.large,
            end = VadretTheme.spacing.large,
            bottom = VadretTheme.spacing.large
        ),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = WEATHER_HEADER_KEY, contentType = DeveloperContentType.HEADER) {
            SettingsSectionHeader(text = stringResource(resource = Res.string.weather))
        }
        item(key = EXPAND_ONCE_KEY, contentType = DeveloperContentType.CARD) {
            VadretCard {
                SwitchRow(
                    title = stringResource(resource = Res.string.allow_expand_once),
                    checkedSummary = stringResource(resource = Res.string.expand_once_is_enabled),
                    uncheckedSummary = stringResource(resource = Res.string.expand_once_is_disabled),
                    isChecked = settings.isExpandOnceEnabled,
                    onCheckedChange = { isChecked: Boolean ->
                        onEvent(SettingsEvent.OnExpandOnceToggled(isExpandOnceEnabled = isChecked))
                    }
                )
            }
        }
        item(key = POLLEN_HEADER_KEY, contentType = DeveloperContentType.HEADER) {
            SettingsSectionHeader(text = stringResource(resource = Res.string.pollen))
        }
        item(key = END_OF_SEASON_KEY, contentType = DeveloperContentType.CARD) {
            VadretCard {
                SwitchRow(
                    title = stringResource(resource = Res.string.simulate_end_of_season),
                    checkedSummary = stringResource(resource = Res.string.end_of_season_is_simulated),
                    uncheckedSummary = stringResource(resource = Res.string.end_of_season_is_not_simulated),
                    isChecked = settings.isEndOfSeasonSimulated,
                    onCheckedChange = { isChecked: Boolean ->
                        onEvent(SettingsEvent.OnEndOfSeasonSimulationToggled(isEndOfSeasonSimulated = isChecked))
                    }
                )
            }
        }
    }
}