package se.vadretapp.vadret.settings.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.allow_expand_once
import se.vadretapp.vadret.core.resources.expand_once_is_disabled
import se.vadretapp.vadret.core.resources.expand_once_is_enabled
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
    }
}