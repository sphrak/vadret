package se.vadretapp.vadret.forecastwarning.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.forecastwarning.WarningStorySimulationEvent
import se.vadretapp.vadret.forecastwarning.WarningStorySimulationState
import se.vadretapp.vadret.forecastwarning.WarningStorySimulationViewModel
import se.vadretapp.vadret.forecastwarning.model.WarningItem
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.ui.LocalHourFormat
import se.vadretapp.vadret.ui.LocalLanguageTag
import se.vadretapp.vadret.ui.SettingsRow
import se.vadretapp.vadret.ui.SettingsRowDivider
import se.vadretapp.vadret.ui.SettingsScaffold
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.ic_play_arrow_filled
import se.vadretapp.vadret.ui.resources.play_all_stories
import se.vadretapp.vadret.ui.resources.simulated_stories

private const val PLAY_ALL_KEY = "play-all"
private const val PLAY_ALL_DIVIDER_KEY = "play-all-divider"

private enum class WarningStorySimulationContentType {
    ROW,
    DIVIDER,
    WARNING
}

@Composable
internal fun WarningStorySimulationRoute(
    onBack: () -> Unit,
    onStoryClick: (warningIdentifiers: List<String>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WarningStorySimulationViewModel = koinViewModel()
) {
    val state: WarningStorySimulationState by viewModel.state.collectAsStateWithLifecycle()
    WarningStorySimulationScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onStoryClick = onStoryClick,
        modifier = modifier
    )
}

@Composable
private fun WarningStorySimulationScreen(
    state: WarningStorySimulationState,
    onEvent: (WarningStorySimulationEvent) -> Unit,
    onBack: () -> Unit,
    onStoryClick: (warningIdentifiers: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val languageTag: String = LocalLanguageTag.current
    val hourFormat: HourFormat = LocalHourFormat.current
    LaunchedEffect(languageTag, hourFormat) {
        onEvent(
            WarningStorySimulationEvent.OnLocaleChanged(
                languageTag = languageTag,
                hourFormat = hourFormat
            )
        )
    }
    SettingsScaffold(
        title = stringResource(resource = Res.string.simulated_stories),
        onBack = onBack,
        modifier = modifier
    ) {
        item(key = PLAY_ALL_KEY, contentType = WarningStorySimulationContentType.ROW) {
            SettingsRow(
                title = stringResource(resource = Res.string.play_all_stories),
                iconResource = Res.drawable.ic_play_arrow_filled,
                onClick = { onStoryClick(state.warningIdentifiers) }
            )
        }
        item(key = PLAY_ALL_DIVIDER_KEY, contentType = WarningStorySimulationContentType.DIVIDER) {
            SettingsRowDivider()
        }
        itemsIndexed(
            items = state.warnings,
            key = { _: Int, warningItem: WarningItem -> warningItem.identifier },
            contentType = { _: Int, _: WarningItem -> WarningStorySimulationContentType.WARNING }
        ) { index: Int, warningItem: WarningItem ->
            Column {
                if (index > 0) {
                    HorizontalDivider(
                        color = VadretTheme.colors.divider,
                        modifier = Modifier.padding(
                            start = rowDividerStartPadding,
                            end = rowDividerEndPadding
                        )
                    )
                }
                WarningRow(
                    warning = warningItem,
                    onClick = { onStoryClick(listOf(warningItem.identifier)) }
                )
            }
        }
    }
}

@Preview(device = Devices.PHONE)
@Composable
private fun WarningStorySimulationScreenPreview(
    @PreviewParameter(provider = WarningPreviewParameterProvider::class) warning: WarningItem
) {
    VadretTheme(theme = Theme.LIGHT) {
        WarningStorySimulationScreen(
            state = WarningStorySimulationState(warnings = listOf(warning).toImmutableList()),
            onEvent = {},
            onBack = {},
            onStoryClick = {}
        )
    }
}