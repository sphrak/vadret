package fi.kroon.vadret.settings.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fi.kroon.vadret.core.resources.Res
import fi.kroon.vadret.core.resources.allow_expand_once
import fi.kroon.vadret.core.resources.expand_once_is_disabled
import fi.kroon.vadret.core.resources.expand_once_is_enabled
import fi.kroon.vadret.core.resources.weather
import fi.kroon.vadret.settings.Settings
import fi.kroon.vadret.settings.SettingsEvent
import fi.kroon.vadret.settings.SettingsState
import fi.kroon.vadret.ui.SettingsSectionHeader
import fi.kroon.vadret.ui.SwitchRow
import fi.kroon.vadret.ui.VadretCard
import fi.kroon.vadret.ui.VadretTheme
import org.jetbrains.compose.resources.stringResource

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