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
import se.vadretapp.vadret.icons.Pollen
import se.vadretapp.vadret.icons.Slipperiness
import se.vadretapp.vadret.icons.VadretIcons
import se.vadretapp.vadret.icons.Warmer
import se.vadretapp.vadret.icons.Warning
import se.vadretapp.vadret.pushalert.PushAlertsEvent
import se.vadretapp.vadret.pushalert.PushAlertsState
import se.vadretapp.vadret.pushalert.PushAlertsViewModel
import se.vadretapp.vadret.pushalert.extension.isNotified
import se.vadretapp.vadret.pushalert.extension.labelResource
import se.vadretapp.vadret.pushalert.model.HourFormat
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PushAlertPlace
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.ChoiceDialog
import se.vadretapp.vadret.ui.InfoDialog
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
import se.vadretapp.vadret.ui.resources.followed_place_position
import se.vadretapp.vadret.ui.resources.hour_format
import se.vadretapp.vadret.ui.resources.ic_schedule
import se.vadretapp.vadret.ui.resources.lowest_pollen_level
import se.vadretapp.vadret.ui.resources.notifications_are_off
import se.vadretapp.vadret.ui.resources.notifications_are_off_text
import se.vadretapp.vadret.ui.resources.notifications_display
import se.vadretapp.vadret.ui.resources.notifications_need_a_place
import se.vadretapp.vadret.ui.resources.notifications_weather
import se.vadretapp.vadret.ui.resources.pollen
import se.vadretapp.vadret.ui.resources.pollen_alerts
import se.vadretapp.vadret.ui.resources.pollen_alerts_need_allergens
import se.vadretapp.vadret.ui.resources.pollen_alerts_summary
import se.vadretapp.vadret.ui.resources.push_alert_setting_not_saved
import se.vadretapp.vadret.ui.resources.push_alerts_not_available
import se.vadretapp.vadret.ui.resources.push_alerts_not_sent
import se.vadretapp.vadret.ui.resources.push_alerts_only_in_sweden
import se.vadretapp.vadret.ui.resources.push_alerts_only_in_sweden_text
import se.vadretapp.vadret.ui.resources.settings_notifications
import se.vadretapp.vadret.ui.resources.settings_places
import se.vadretapp.vadret.ui.resources.slipperiness_alerts
import se.vadretapp.vadret.ui.resources.slipperiness_alerts_summary
import se.vadretapp.vadret.ui.resources.strong_wind_alerts
import se.vadretapp.vadret.ui.resources.temperature_alerts
import se.vadretapp.vadret.ui.resources.warning_alerts
import se.vadretapp.vadret.ui.resources.warning_alerts_summary
import se.vadretapp.vadret.ui.resources.warning_level
import se.vadretapp.vadret.ui.resources.warnings

private const val SYNC_PENDING_KEY = "sync-pending"
private const val WEATHER_HEADER_KEY = "weather-header"
private const val WARNINGS_HEADER_KEY = "warnings-header"
private const val WARNINGS_KEY = "warnings"
private const val WARNING_LEVEL_KEY = "warning-level"
private const val TEMPERATURE_KEY = "temperature"
private const val STRONG_WIND_KEY = "strong-wind"
private const val SLIPPERINESS_KEY = "slipperiness"
private const val POLLEN_HEADER_KEY = "pollen-header"
private const val POLLEN_KEY = "pollen"
private const val POLLEN_LEVEL_KEY = "pollen-level"
private const val DISPLAY_HEADER_KEY = "display-header"
private const val HOUR_FORMAT_KEY = "hour-format"
private const val PLACES_HEADER_KEY = "places-header"
private const val CHOSEN_PLACE_KEY = "chosen-place"
private const val SAVED_PLACE_KEY_PREFIX = "place-"

private enum class PushAlertsContentType {
    NOTE,
    HEADER,
    ROW
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
    warningAlerts(
        state = state,
        onEvent = onEvent
    )
    pushAlertPlaces(
        state = state,
        onEvent = onEvent
    )
    pollenAlerts(
        state = state,
        onEvent = onEvent
    )
    display(
        state = state,
        onEvent = onEvent
    )
}

private fun LazyListScope.weatherAlerts(
    state: PushAlertsState,
    onEvent: (PushAlertsEvent) -> Unit
) {
    item(key = WEATHER_HEADER_KEY, contentType = PushAlertsContentType.HEADER) {
        SettingsSectionHeader(text = stringResource(resource = Res.string.notifications_weather))
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
            isChecked = state.choices.strongWind,
            onCheckedChange = { isChecked: Boolean -> onEvent(PushAlertsEvent.OnStrongWindToggled(isEnabled = isChecked)) },
            leadingContent = { SettingsRowIcon(icon = VadretIcons.Alert.HardWind) }
        )
    }
    item(key = SLIPPERINESS_KEY, contentType = PushAlertsContentType.ROW) {
        SwitchRow(
            title = stringResource(resource = Res.string.slipperiness_alerts),
            summary = stringResource(resource = Res.string.slipperiness_alerts_summary),
            isChecked = state.choices.slipperiness,
            onCheckedChange = { isChecked: Boolean -> onEvent(PushAlertsEvent.OnSlipperinessToggled(isEnabled = isChecked)) },
            leadingContent = { SettingsRowIcon(icon = VadretIcons.Alert.Slipperiness) }
        )
    }
}

private fun LazyListScope.warningAlerts(
    state: PushAlertsState,
    onEvent: (PushAlertsEvent) -> Unit
) {
    val warningLowestLevel: WarningAlertLevel? = state.choices.warningLowestLevel
    item(key = WARNINGS_HEADER_KEY, contentType = PushAlertsContentType.HEADER) {
        SettingsSectionHeader(text = stringResource(resource = Res.string.warnings))
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
}

private fun LazyListScope.pollenAlerts(
    state: PushAlertsState,
    onEvent: (PushAlertsEvent) -> Unit
) {
    val pollenLowestLevel: PollenAlertLevel? = state.choices.pollenLowestLevel
    item(key = POLLEN_HEADER_KEY, contentType = PushAlertsContentType.HEADER) {
        SettingsSectionHeader(text = stringResource(resource = Res.string.pollen))
    }
    item(key = POLLEN_KEY, contentType = PushAlertsContentType.ROW) {
        SwitchRow(
            title = stringResource(resource = Res.string.pollen_alerts),
            summary = stringResource(resource = Res.string.pollen_alerts_summary),
            isChecked = pollenLowestLevel != null,
            onCheckedChange = { isChecked: Boolean -> onEvent(PushAlertsEvent.OnPollenAlertsToggled(isEnabled = isChecked)) },
            leadingContent = { SettingsRowIcon(icon = VadretIcons.Alert.Pollen) }
        )
    }
    if (pollenLowestLevel != null) {
        item(key = POLLEN_LEVEL_KEY, contentType = PushAlertsContentType.ROW) {
            SettingsRow(
                title = stringResource(resource = Res.string.lowest_pollen_level),
                summary = stringResource(resource = pollenLowestLevel.labelResource()),
                onClick = { onEvent(PushAlertsEvent.OnPollenLevelRowClicked) },
                leadingContent = { SettingsRowIconSpace() }
            )
        }
    }
}

private fun LazyListScope.display(
    state: PushAlertsState,
    onEvent: (PushAlertsEvent) -> Unit
) {
    item(key = DISPLAY_HEADER_KEY, contentType = PushAlertsContentType.HEADER) {
        SettingsSectionHeader(text = stringResource(resource = Res.string.notifications_display))
    }
    item(key = HOUR_FORMAT_KEY, contentType = PushAlertsContentType.ROW) {
        SettingsRow(
            title = stringResource(resource = Res.string.hour_format),
            summary = stringResource(resource = state.choices.hourFormat.labelResource()),
            iconResource = Res.drawable.ic_schedule,
            onClick = { onEvent(PushAlertsEvent.OnHourFormatRowClicked) }
        )
    }
}

private fun LazyListScope.pushAlertPlaces(
    state: PushAlertsState,
    onEvent: (PushAlertsEvent) -> Unit
) {
    item(key = PLACES_HEADER_KEY, contentType = PushAlertsContentType.HEADER) {
        SettingsSectionHeader(text = stringResource(resource = Res.string.settings_places))
    }
    items(
        items = state.places,
        key = { pushAlertPlace: PushAlertPlace -> pushAlertPlace.key() },
        contentType = { PushAlertsContentType.ROW }
    ) { pushAlertPlace: PushAlertPlace ->
        SwitchRow(
            title = pushAlertPlace.title(),
            isChecked = state.choices.isNotified(pushAlertPlace = pushAlertPlace),
            onCheckedChange = { isChecked: Boolean ->
                onEvent(
                    PushAlertsEvent.OnPlaceToggled(
                        pushAlertPlace = pushAlertPlace,
                        isEnabled = isChecked
                    )
                )
            },
            leadingContent = { SettingsRowIconSpace() }
        )
    }
}

private fun PushAlertPlace.key(): String =
    when (this) {
        is PushAlertPlace.ChosenPlace -> CHOSEN_PLACE_KEY
        is PushAlertPlace.SavedPlace -> "$SAVED_PLACE_KEY_PREFIX$favoriteId"
    }

@Composable
private fun PushAlertPlace.title(): String =
    when (this) {
        is PushAlertPlace.ChosenPlace -> name ?: stringResource(resource = Res.string.followed_place_position)
        is PushAlertPlace.SavedPlace -> name
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
    val pollenLowestLevel: PollenAlertLevel? = state.choices.pollenLowestLevel
    if (state.isPollenLevelDialogVisible && pollenLowestLevel != null) {
        ChoiceDialog(
            title = stringResource(resource = Res.string.lowest_pollen_level),
            choices = PollenAlertLevel.entries,
            selectedChoice = pollenLowestLevel,
            choiceLabel = { pollenAlertLevel: PollenAlertLevel -> stringResource(resource = pollenAlertLevel.labelResource()) },
            onChoiceSelected = { pollenAlertLevel: PollenAlertLevel -> onEvent(PushAlertsEvent.OnPollenLevelSelected(pollenAlertLevel = pollenAlertLevel)) },
            onDismiss = { onEvent(PushAlertsEvent.OnPollenLevelDialogDismissed) },
            modifier = modifier
        )
    }
    if (state.isHourFormatDialogVisible) {
        ChoiceDialog(
            title = stringResource(resource = Res.string.hour_format),
            choices = HourFormat.entries,
            selectedChoice = state.choices.hourFormat,
            choiceLabel = { hourFormat: HourFormat -> stringResource(resource = hourFormat.labelResource()) },
            onChoiceSelected = { hourFormat: HourFormat -> onEvent(PushAlertsEvent.OnHourFormatSelected(hourFormat = hourFormat)) },
            onDismiss = { onEvent(PushAlertsEvent.OnHourFormatDialogDismissed) },
            modifier = modifier
        )
    }
    if (state.isNoAllergensDialogVisible) {
        InfoDialog(
            title = stringResource(resource = Res.string.pollen_alerts),
            text = stringResource(resource = Res.string.pollen_alerts_need_allergens),
            onDismiss = { onEvent(PushAlertsEvent.OnNoAllergensDialogDismissed) },
            modifier = modifier
        )
    }
    if (state.isLastPlaceDialogVisible) {
        InfoDialog(
            title = stringResource(resource = Res.string.settings_notifications),
            text = stringResource(resource = Res.string.notifications_need_a_place),
            onDismiss = { onEvent(PushAlertsEvent.OnLastPlaceDialogDismissed) },
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