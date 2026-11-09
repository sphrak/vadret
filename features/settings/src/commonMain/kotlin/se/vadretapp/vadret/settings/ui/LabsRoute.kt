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
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.icons.Aurora
import se.vadretapp.vadret.icons.VadretIcons
import se.vadretapp.vadret.setting.model.Theme
import se.vadretapp.vadret.settings.LabsEvent
import se.vadretapp.vadret.settings.LabsState
import se.vadretapp.vadret.settings.LabsViewModel
import se.vadretapp.vadret.settings.model.Labs
import se.vadretapp.vadret.ui.InfoDialog
import se.vadretapp.vadret.ui.SettingsFootnote
import se.vadretapp.vadret.ui.SettingsRowIcon
import se.vadretapp.vadret.ui.SettingsScaffold
import se.vadretapp.vadret.ui.SwitchRow
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.extension.messageResource
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.aurora_alerts
import se.vadretapp.vadret.ui.resources.ic_bottom_navigation
import se.vadretapp.vadret.ui.resources.ic_schedule
import se.vadretapp.vadret.ui.resources.labs
import se.vadretapp.vadret.ui.resources.labs_aurora_summary
import se.vadretapp.vadret.ui.resources.labs_footnote
import se.vadretapp.vadret.ui.resources.settings_hourly_forecast
import se.vadretapp.vadret.ui.resources.settings_hourly_forecast_summary
import se.vadretapp.vadret.ui.resources.settings_native_navigation
import se.vadretapp.vadret.ui.resources.settings_native_navigation_summary

private const val FOOTNOTE_KEY = "footnote"
private const val HOURLY_FORECAST_KEY = "hourly-forecast"
private const val AURORA_KEY = "aurora"
private const val NATIVE_NAVIGATION_KEY = "native-navigation"

private enum class LabsContentType {
    TEXT,
    ROW
}

@Composable
fun LabsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LabsViewModel = koinViewModel()
) {
    val state: LabsState by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.onEvent(event = LabsEvent.OnStart)
    }
    LabsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun LabsScreen(
    state: LabsState,
    onEvent: (LabsEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val labs: Labs? = state.labs
    val isNativeNavigationEnabled: Boolean? = state.isNativeNavigationEnabled
    val failure: Failure? = state.failure
    SettingsScaffold(
        title = stringResource(resource = Res.string.labs),
        onBack = onBack,
        modifier = modifier
    ) {
        if (labs != null) {
            item(key = FOOTNOTE_KEY, contentType = LabsContentType.TEXT) {
                SettingsFootnote(text = stringResource(resource = Res.string.labs_footnote))
            }
            item(key = HOURLY_FORECAST_KEY, contentType = LabsContentType.ROW) {
                SwitchRow(
                    title = stringResource(resource = Res.string.settings_hourly_forecast),
                    summary = stringResource(resource = Res.string.settings_hourly_forecast_summary),
                    iconResource = Res.drawable.ic_schedule,
                    isChecked = labs.isHourlyForecastEnabled,
                    onCheckedChange = { isChecked: Boolean -> onEvent(LabsEvent.OnHourlyForecastToggled(isHourlyForecastEnabled = isChecked)) }
                )
            }
            item(key = AURORA_KEY, contentType = LabsContentType.ROW) {
                SwitchRow(
                    title = stringResource(resource = Res.string.aurora_alerts),
                    summary = stringResource(resource = Res.string.labs_aurora_summary),
                    isChecked = labs.isAuroraEnabled,
                    onCheckedChange = { isChecked: Boolean -> onEvent(LabsEvent.OnAuroraToggled(isAuroraEnabled = isChecked)) },
                    leadingContent = { SettingsRowIcon(icon = VadretIcons.Alert.Aurora) }
                )
            }
        }
        if (isNativeNavigationEnabled != null) {
            item(key = NATIVE_NAVIGATION_KEY, contentType = LabsContentType.ROW) {
                SwitchRow(
                    title = stringResource(resource = Res.string.settings_native_navigation),
                    summary = stringResource(resource = Res.string.settings_native_navigation_summary),
                    iconResource = Res.drawable.ic_bottom_navigation,
                    isChecked = isNativeNavigationEnabled,
                    onCheckedChange = { isChecked: Boolean -> onEvent(LabsEvent.OnNativeNavigationToggled(isNativeNavigationEnabled = isChecked)) }
                )
            }
        }
    }
    if (failure != null) {
        InfoDialog(
            title = stringResource(resource = Res.string.labs),
            text = stringResource(resource = failure.messageResource()),
            onDismiss = { onEvent(LabsEvent.OnFailureDismissed) }
        )
    }
}

@Preview(device = Devices.PHONE)
@Composable
private fun LabsScreenPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        LabsScreen(
            state = labsPreviewState(theme = theme),
            onEvent = {},
            onBack = {},
            modifier = Modifier.background(color = VadretTheme.colorScheme.background)
        )
    }
}