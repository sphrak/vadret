package se.vadretapp.vadret.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.settings.PollenSettingsEvent
import se.vadretapp.vadret.settings.PollenSettingsState
import se.vadretapp.vadret.settings.PollenSettingsViewModel
import se.vadretapp.vadret.settings.model.PollenAllergenRow
import se.vadretapp.vadret.settings.model.PollenGroupRows
import se.vadretapp.vadret.ui.SettingsFootnote
import se.vadretapp.vadret.ui.SettingsNote
import se.vadretapp.vadret.ui.SettingsRowDivider
import se.vadretapp.vadret.ui.SettingsScaffold
import se.vadretapp.vadret.ui.SettingsSectionHeader
import se.vadretapp.vadret.ui.SwitchRow
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.ic_check
import se.vadretapp.vadret.ui.resources.pollen
import se.vadretapp.vadret.ui.resources.pollen_marked_first
import se.vadretapp.vadret.ui.resources.pollen_off_season
import se.vadretapp.vadret.ui.resources.pollen_on_map
import se.vadretapp.vadret.ui.resources.pollen_on_map_summary
import se.vadretapp.vadret.ui.resources.pollen_types_not_available

private const val ON_MAP_KEY = "on-map"
private const val OFF_SEASON_KEY = "off-season"
private const val DIVIDER_KEY = "divider"
private const val MARKED_FIRST_KEY = "marked-first"
private const val UNAVAILABLE_KEY = "unavailable"

private enum class PollenContentType {
    ROW,
    NOTE,
    DIVIDER,
    TEXT,
    GROUP
}

@Composable
fun PollenSettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PollenSettingsViewModel = koinViewModel()
) {
    val state: PollenSettingsState by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.onEvent(event = PollenSettingsEvent.OnStart)
    }
    PollenSettingsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun PollenSettingsScreen(
    state: PollenSettingsState,
    onEvent: (PollenSettingsEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScaffold(
        title = stringResource(resource = Res.string.pollen),
        onBack = onBack,
        modifier = modifier
    ) {
        if (!state.isLoading) {
            pollenSettings(
                state = state,
                onEvent = onEvent
            )
        }
    }
}

private fun LazyListScope.pollenSettings(
    state: PollenSettingsState,
    onEvent: (PollenSettingsEvent) -> Unit
) {
    val isPollenEnabled: Boolean? = state.isPollenEnabled
    if (isPollenEnabled != null) {
        item(key = ON_MAP_KEY, contentType = PollenContentType.ROW) {
            SwitchRow(
                title = stringResource(resource = Res.string.pollen_on_map),
                summary = stringResource(resource = Res.string.pollen_on_map_summary),
                isChecked = isPollenEnabled,
                onCheckedChange = { isChecked: Boolean -> onEvent(PollenSettingsEvent.OnPollenToggled(isPollenEnabled = isChecked)) }
            )
        }
    }
    if (state.isOffSeason) {
        item(key = OFF_SEASON_KEY, contentType = PollenContentType.NOTE) {
            SettingsNote(text = stringResource(resource = Res.string.pollen_off_season))
        }
    }
    if (isPollenEnabled == true) {
        item(key = DIVIDER_KEY, contentType = PollenContentType.DIVIDER) {
            SettingsRowDivider()
        }
        item(key = MARKED_FIRST_KEY, contentType = PollenContentType.TEXT) {
            SettingsFootnote(text = stringResource(resource = Res.string.pollen_marked_first))
        }
        if (state.arePollenTypesUnavailable) {
            item(key = UNAVAILABLE_KEY, contentType = PollenContentType.TEXT) {
                SettingsFootnote(text = stringResource(resource = Res.string.pollen_types_not_available))
            }
        }
        items(
            items = state.pollenGroupRows,
            key = { pollenGroupRows: PollenGroupRows -> pollenGroupRows.pollenGroup.name },
            contentType = { PollenContentType.GROUP }
        ) { pollenGroupRows: PollenGroupRows ->
            PollenGroupChips(
                pollenGroupRows = pollenGroupRows,
                onEvent = onEvent
            )
        }
    }
}

@Composable
private fun PollenGroupChips(
    pollenGroupRows: PollenGroupRows,
    onEvent: (PollenSettingsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SettingsSectionHeader(text = stringResource(resource = pollenGroupRows.pollenGroup.titleResource))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(space = VadretTheme.spacing.small),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VadretTheme.spacing.large)
        ) {
            pollenGroupRows.pollenAllergenRows.forEach { pollenAllergenRow: PollenAllergenRow ->
                FilterChip(
                    selected = pollenAllergenRow.isAllergen,
                    onClick = {
                        onEvent(
                            PollenSettingsEvent.OnPollenMarked(
                                pollenId = pollenAllergenRow.pollenId,
                                isMarked = !pollenAllergenRow.isAllergen
                            )
                        )
                    },
                    label = { Text(text = pollenAllergenRow.name) },
                    leadingIcon = if (pollenAllergenRow.isAllergen) {
                        {
                            Icon(
                                painter = painterResource(resource = Res.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.size(size = FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@Preview(device = Devices.PHONE)
@Composable
private fun PollenSettingsScreenPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        PollenSettingsScreen(
            state = pollenSettingsPreviewState(),
            onEvent = {},
            onBack = {},
            modifier = Modifier.background(color = VadretTheme.colorScheme.background)
        )
    }
}