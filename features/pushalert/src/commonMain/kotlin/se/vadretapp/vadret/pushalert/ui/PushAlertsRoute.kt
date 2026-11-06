package se.vadretapp.vadret.pushalert.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.icons.HardWind
import se.vadretapp.vadret.icons.VadretIcons
import se.vadretapp.vadret.icons.Warmer
import se.vadretapp.vadret.icons.Warning
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pushalert.PushAlertsEvent
import se.vadretapp.vadret.pushalert.PushAlertsState
import se.vadretapp.vadret.pushalert.PushAlertsViewModel
import se.vadretapp.vadret.pushalert.extension.labelResource
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.ChoiceDialog
import se.vadretapp.vadret.ui.InfoDialog
import se.vadretapp.vadret.ui.SettingsFootnote
import se.vadretapp.vadret.ui.SettingsNote
import se.vadretapp.vadret.ui.SettingsRow
import se.vadretapp.vadret.ui.SettingsRowIcon
import se.vadretapp.vadret.ui.SettingsRowIconSpace
import se.vadretapp.vadret.ui.SettingsScaffold
import se.vadretapp.vadret.ui.SettingsSectionHeader
import se.vadretapp.vadret.ui.SwitchRow
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.extension.messageResource
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.attestation_refused
import se.vadretapp.vadret.ui.resources.forecast_alerts_all_places
import se.vadretapp.vadret.ui.resources.forecast_alerts_all_places_summary
import se.vadretapp.vadret.ui.resources.lowest_pollen_level
import se.vadretapp.vadret.ui.resources.notifications_are_off
import se.vadretapp.vadret.ui.resources.notifications_are_off_text
import se.vadretapp.vadret.ui.resources.notifications_cover_places
import se.vadretapp.vadret.ui.resources.notifications_cover_places_except_forecast
import se.vadretapp.vadret.ui.resources.notifications_weather
import se.vadretapp.vadret.ui.resources.pollen
import se.vadretapp.vadret.ui.resources.pollen_types_not_available
import se.vadretapp.vadret.ui.resources.push_alert_setting_not_saved
import se.vadretapp.vadret.ui.resources.push_alerts_not_available
import se.vadretapp.vadret.ui.resources.push_alerts_not_sent
import se.vadretapp.vadret.ui.resources.push_alerts_only_in_sweden
import se.vadretapp.vadret.ui.resources.push_alerts_only_in_sweden_text
import se.vadretapp.vadret.ui.resources.settings_notifications
import se.vadretapp.vadret.ui.resources.strong_wind_alerts
import se.vadretapp.vadret.ui.resources.strong_wind_alerts_summary
import se.vadretapp.vadret.ui.resources.temperature_alerts
import se.vadretapp.vadret.ui.resources.warning_alerts
import se.vadretapp.vadret.ui.resources.warning_alerts_summary
import se.vadretapp.vadret.ui.resources.warning_level

private const val SYNC_PENDING_KEY = "sync-pending"
private const val WEATHER_HEADER_KEY = "weather-header"
private const val WARNINGS_KEY = "warnings"
private const val WARNING_LEVEL_KEY = "warning-level"
private const val TEMPERATURE_KEY = "temperature"
private const val STRONG_WIND_KEY = "strong-wind"
private const val FORECAST_PLACES_KEY = "forecast-places"
private const val POLLEN_HEADER_KEY = "pollen-header"
private const val POLLEN_UNAVAILABLE_KEY = "pollen-unavailable"
private const val POLLEN_LEVEL_KEY = "pollen-level"
private const val PLACES_KEY = "places"

private enum class PushAlertsContentType {
    NOTE,
    HEADER,
    ROW,
    TEXT
}

@Composable
internal fun PushAlertsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PushAlertsViewModel = koinViewModel()
) {
    val state: PushAlertsState by viewModel.state.collectAsStateWithLifecycle()
    val requestNotificationPermission: () -> Unit = rememberNotificationPermissionRequest { isGranted: Boolean ->
        viewModel.onEvent(event = PushAlertsEvent.OnNotificationPermissionResult(isGranted = isGranted))
    }
    LaunchedEffect(Unit) {
        viewModel.onEvent(event = PushAlertsEvent.OnStart)
    }
    LaunchedEffect(state.pendingChoices) {
        if (state.pendingChoices != null) {
            requestNotificationPermission()
        }
    }
    PushAlertsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun PushAlertsScreen(
    state: PushAlertsState,
    onEvent: (PushAlertsEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val failure: Failure? = state.failure
    SettingsScaffold(
        title = stringResource(resource = Res.string.settings_notifications),
        onBack = onBack,
        modifier = modifier
    ) {
        if (!state.isLoading) {
            pushAlerts(
                state = state,
                onEvent = onEvent
            )
        }
    }
    if (failure != null) {
        InfoDialog(
            title = stringResource(resource = Res.string.settings_notifications),
            text = stringResource(resource = failure.messageResource()),
            onDismiss = { onEvent(PushAlertsEvent.OnFailureDismissed) }
        )
    }
    if (state.isSendFailedDialogVisible) {
        InfoDialog(
            title = stringResource(resource = Res.string.settings_notifications),
            text = stringResource(resource = Res.string.push_alert_setting_not_saved),
            onDismiss = { onEvent(PushAlertsEvent.OnSendFailedDialogDismissed) }
        )
    }

    PushAlertsDialogs(
        state = state,
        onEvent = onEvent
    )
}

private fun LazyListScope.pushAlerts(
    state: PushAlertsState,
    onEvent: (PushAlertsEvent) -> Unit
) {
    if (state.isSyncPending) {
        item(key = SYNC_PENDING_KEY, contentType = PushAlertsContentType.NOTE) {
            SettingsNote(text = stringResource(resource = Res.string.push_alerts_not_sent))
        }
    }
    weatherAlerts(
        state = state,
        onEvent = onEvent
    )
    if (state.pollenTypes.isNotEmpty() || state.arePollenTypesUnavailable) {
        pollenAlerts(
            state = state,
            onEvent = onEvent
        )
    }
    item(key = PLACES_KEY, contentType = PushAlertsContentType.TEXT) {
        SettingsFootnote(
            text = stringResource(
                resource = if (state.choices.forecastForAllPlaces) {
                    Res.string.notifications_cover_places
                } else {
                    Res.string.notifications_cover_places_except_forecast
                }
            )
        )
    }
}

private fun LazyListScope.weatherAlerts(
    state: PushAlertsState,
    onEvent: (PushAlertsEvent) -> Unit
) {
    val warningLowestLevel: WarningAlertLevel? = state.choices.warningLowestLevel
    item(key = WEATHER_HEADER_KEY, contentType = PushAlertsContentType.HEADER) {
        SettingsSectionHeader(text = stringResource(resource = Res.string.notifications_weather))
    }
    item(key = WARNINGS_KEY, contentType = PushAlertsContentType.ROW) {
        SwitchRow(
            title = stringResource(resource = Res.string.warning_alerts),
            summary = stringResource(resource = Res.string.warning_alerts_summary),
            isChecked = warningLowestLevel != null,
            onCheckedChange = { isChecked: Boolean -> onEvent(PushAlertsEvent.OnWarningAlertsToggled(isEnabled = isChecked)) },
            leadingContent = { SettingsRowIcon(icon = VadretIcons.Alert.Warning) }
        )
    }
    if (warningLowestLevel != null) {
        item(key = WARNING_LEVEL_KEY, contentType = PushAlertsContentType.ROW) {
            SettingsRow(
                title = stringResource(resource = Res.string.warning_level),
                summary = stringResource(resource = warningLowestLevel.labelResource()),
                onClick = { onEvent(PushAlertsEvent.OnWarningLevelRowClicked) },
                leadingContent = { SettingsRowIconSpace() }
            )
        }
    }
    item(key = TEMPERATURE_KEY, contentType = PushAlertsContentType.ROW) {
        SwitchRow(
            title = stringResource(resource = Res.string.temperature_alerts),
            isChecked = state.choices.temperatureTrend,
            onCheckedChange = { isChecked: Boolean -> onEvent(PushAlertsEvent.OnTemperatureTrendToggled(isEnabled = isChecked)) },
            leadingContent = { SettingsRowIcon(icon = VadretIcons.Alert.Warmer) }
        )
    }
    item(key = STRONG_WIND_KEY, contentType = PushAlertsContentType.ROW) {
        SwitchRow(
            title = stringResource(resource = Res.string.strong_wind_alerts),
            summary = stringResource(resource = Res.string.strong_wind_alerts_summary),
            isChecked = state.choices.strongWind,
            onCheckedChange = { isChecked: Boolean -> onEvent(PushAlertsEvent.OnStrongWindToggled(isEnabled = isChecked)) },
            leadingContent = { SettingsRowIcon(icon = VadretIcons.Alert.HardWind) }
        )
    }
    if (state.choices.temperatureTrend || state.choices.strongWind) {
        item(key = FORECAST_PLACES_KEY, contentType = PushAlertsContentType.ROW) {
            SwitchRow(
                title = stringResource(resource = Res.string.forecast_alerts_all_places),
                summary = stringResource(resource = Res.string.forecast_alerts_all_places_summary),
                isChecked = state.choices.forecastForAllPlaces,
                onCheckedChange = { isChecked: Boolean -> onEvent(PushAlertsEvent.OnForecastForAllPlacesToggled(isEnabled = isChecked)) },
                leadingContent = { SettingsRowIconSpace() }
            )
        }
    }
}

private fun LazyListScope.pollenAlerts(
    state: PushAlertsState,
    onEvent: (PushAlertsEvent) -> Unit
) {
    item(key = POLLEN_HEADER_KEY, contentType = PushAlertsContentType.HEADER) {
        SettingsSectionHeader(text = stringResource(resource = Res.string.pollen))
    }
    if (state.arePollenTypesUnavailable) {
        item(key = POLLEN_UNAVAILABLE_KEY, contentType = PushAlertsContentType.TEXT) {
            SettingsFootnote(text = stringResource(resource = Res.string.pollen_types_not_available))
        }
    }
    items(
        items = state.pollenTypes,
        key = { pollenType: PollenType -> pollenType.pollenId },
        contentType = { PushAlertsContentType.ROW }
    ) { pollenType: PollenType ->
        SwitchRow(
            title = pollenType.name,
            isChecked = pollenType.pollenId in state.chosenPollenTypeIds,
            onCheckedChange = { isChecked: Boolean ->
                onEvent(
                    PushAlertsEvent.OnPollenTypeToggled(
                        pollenTypeId = pollenType.pollenId,
                        isEnabled = isChecked
                    )
                )
            },
            leadingContent = { SettingsRowIconSpace() }
        )
    }
    if (state.chosenPollenTypeIds.isNotEmpty()) {
        item(key = POLLEN_LEVEL_KEY, contentType = PushAlertsContentType.ROW) {
            SettingsRow(
                title = stringResource(resource = Res.string.lowest_pollen_level),
                summary = stringResource(resource = state.pollenLevel.labelResource()),
                onClick = { onEvent(PushAlertsEvent.OnPollenLevelRowClicked) },
                leadingContent = { SettingsRowIconSpace() }
            )
        }
    }
}

@Composable
private fun PushAlertsDialogs(
    state: PushAlertsState,
    onEvent: (PushAlertsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val warningLowestLevel: WarningAlertLevel? = state.choices.warningLowestLevel
    if (state.isWarningLevelDialogVisible && warningLowestLevel != null) {
        ChoiceDialog(
            title = stringResource(resource = Res.string.warning_level),
            choices = WarningAlertLevel.entries,
            selectedChoice = warningLowestLevel,
            choiceLabel = { warningAlertLevel: WarningAlertLevel -> stringResource(resource = warningAlertLevel.labelResource()) },
            onChoiceSelected = { warningAlertLevel: WarningAlertLevel -> onEvent(PushAlertsEvent.OnWarningLevelSelected(warningAlertLevel = warningAlertLevel)) },
            onDismiss = { onEvent(PushAlertsEvent.OnWarningLevelDialogDismissed) },
            modifier = modifier
        )
    }
    if (state.isPollenLevelDialogVisible) {
        ChoiceDialog(
            title = stringResource(resource = Res.string.lowest_pollen_level),
            choices = PollenAlertLevel.entries,
            selectedChoice = state.pollenLevel,
            choiceLabel = { pollenAlertLevel: PollenAlertLevel -> stringResource(resource = pollenAlertLevel.labelResource()) },
            onChoiceSelected = { pollenAlertLevel: PollenAlertLevel -> onEvent(PushAlertsEvent.OnPollenLevelSelected(pollenAlertLevel = pollenAlertLevel)) },
            onDismiss = { onEvent(PushAlertsEvent.OnPollenLevelDialogDismissed) },
            modifier = modifier
        )
    }
    if (state.isNotificationPermissionDialogVisible) {
        InfoDialog(
            title = stringResource(resource = Res.string.notifications_are_off),
            text = stringResource(resource = Res.string.notifications_are_off_text),
            onDismiss = { onEvent(PushAlertsEvent.OnNotificationPermissionDialogDismissed) },
            modifier = modifier
        )
    }
    if (state.isOutsideSwedenDialogVisible) {
        InfoDialog(
            title = stringResource(resource = Res.string.push_alerts_only_in_sweden),
            text = stringResource(resource = Res.string.push_alerts_only_in_sweden_text),
            onDismiss = { onEvent(PushAlertsEvent.OnOutsideSwedenDialogDismissed) },
            modifier = modifier
        )
    }
    if (state.isAttestationRefusedDialogVisible) {
        InfoDialog(
            title = stringResource(resource = Res.string.push_alerts_not_available),
            text = stringResource(resource = Res.string.attestation_refused),
            onDismiss = { onEvent(PushAlertsEvent.OnAttestationRefusedDialogDismissed) },
            modifier = modifier
        )
    }
}

@Preview
@Composable
private fun PushAlertsScreenPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        PushAlertsScreen(
            state = pushAlertsPreviewState(),
            onEvent = {},
            onBack = {},
            modifier = Modifier.background(color = VadretTheme.colorScheme.background)
        )
    }
}