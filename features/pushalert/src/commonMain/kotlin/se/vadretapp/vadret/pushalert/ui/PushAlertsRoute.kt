package se.vadretapp.vadret.pushalert.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.core.resources.Res
import se.vadretapp.vadret.core.resources.attestation_refused
import se.vadretapp.vadret.core.resources.ic_chevron_right
import se.vadretapp.vadret.core.resources.lowest_pollen_level
import se.vadretapp.vadret.core.resources.lowest_warning_level
import se.vadretapp.vadret.core.resources.notifications_are_off
import se.vadretapp.vadret.core.resources.notifications_are_off_text
import se.vadretapp.vadret.core.resources.ok
import se.vadretapp.vadret.core.resources.pollen_alerts
import se.vadretapp.vadret.core.resources.pollen_alerts_off
import se.vadretapp.vadret.core.resources.pollen_alerts_on
import se.vadretapp.vadret.core.resources.push_alerts
import se.vadretapp.vadret.core.resources.push_alerts_not_available
import se.vadretapp.vadret.core.resources.push_alerts_not_sent
import se.vadretapp.vadret.core.resources.push_alerts_only_in_sweden
import se.vadretapp.vadret.core.resources.push_alerts_only_in_sweden_text
import se.vadretapp.vadret.core.resources.temperature_alerts
import se.vadretapp.vadret.core.resources.temperature_alerts_off
import se.vadretapp.vadret.core.resources.temperature_alerts_on
import se.vadretapp.vadret.core.resources.warning_alerts
import se.vadretapp.vadret.core.resources.warning_alerts_off
import se.vadretapp.vadret.core.resources.warning_alerts_on
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pushalert.PushAlertsEvent
import se.vadretapp.vadret.pushalert.PushAlertsState
import se.vadretapp.vadret.pushalert.PushAlertsViewModel
import se.vadretapp.vadret.pushalert.extension.labelResource
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.ChoiceDialog
import se.vadretapp.vadret.ui.SettingsRowDivider
import se.vadretapp.vadret.ui.SettingsSectionHeader
import se.vadretapp.vadret.ui.SettingsValueRow
import se.vadretapp.vadret.ui.SwitchRow
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretCard
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.extension.messageResource

@Composable
internal fun PushAlertsRoute(
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
        modifier = modifier
    )
}

@Composable
internal fun PushAlertsScreen(
    state: PushAlertsState,
    onEvent: (PushAlertsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SettingsSectionHeader(text = stringResource(resource = Res.string.push_alerts))
        if (state.isSyncPending) {
            Text(
                text = stringResource(resource = Res.string.push_alerts_not_sent),
                style = VadretTheme.textStyles.body,
                color = VadretTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(all = VadretTheme.spacing.large)
            )
        }
        WeatherAlertsCard(
            state = state,
            onEvent = onEvent
        )
        if (state.pollenTypes.isNotEmpty()) {
            SettingsSectionHeader(text = stringResource(resource = Res.string.pollen_alerts))
            PollenAlertsCard(
                state = state,
                onEvent = onEvent
            )
        }
        val failure: Failure? = state.failure
        if (failure != null) {
            Text(
                text = stringResource(resource = failure.messageResource()),
                color = VadretTheme.colorScheme.error,
                modifier = Modifier
                    .clickable { onEvent(PushAlertsEvent.OnFailureDismissed) }
                    .padding(all = VadretTheme.spacing.large)
            )
        }
    }

    PushAlertsDialogs(
        state = state,
        onEvent = onEvent
    )
}

@Composable
private fun WeatherAlertsCard(
    state: PushAlertsState,
    onEvent: (PushAlertsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val warningLowestLevel: WarningAlertLevel? = state.choices.warningLowestLevel
    VadretCard(modifier = modifier) {
        SwitchRow(
            title = stringResource(resource = Res.string.warning_alerts),
            checkedSummary = stringResource(resource = Res.string.warning_alerts_on),
            uncheckedSummary = stringResource(resource = Res.string.warning_alerts_off),
            isChecked = warningLowestLevel != null,
            onCheckedChange = { isChecked: Boolean -> onEvent(PushAlertsEvent.OnWarningAlertsToggled(isEnabled = isChecked)) }
        )
        if (warningLowestLevel != null) {
            SettingsRowDivider()
            SettingsValueRow(
                title = stringResource(resource = Res.string.lowest_warning_level),
                value = stringResource(resource = warningLowestLevel.labelResource()),
                trailingIconResource = Res.drawable.ic_chevron_right,
                onClick = { onEvent(PushAlertsEvent.OnWarningLevelRowClicked) }
            )
        }
        SettingsRowDivider()
        SwitchRow(
            title = stringResource(resource = Res.string.temperature_alerts),
            checkedSummary = stringResource(resource = Res.string.temperature_alerts_on),
            uncheckedSummary = stringResource(resource = Res.string.temperature_alerts_off),
            isChecked = state.choices.temperatureTrend,
            onCheckedChange = { isChecked: Boolean -> onEvent(PushAlertsEvent.OnTemperatureTrendToggled(isEnabled = isChecked)) }
        )
    }
}

@Composable
private fun PollenAlertsCard(
    state: PushAlertsState,
    onEvent: (PushAlertsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    VadretCard(modifier = modifier) {
        state.pollenTypes.forEachIndexed { index: Int, pollenType: PollenType ->
            if (index > 0) {
                SettingsRowDivider()
            }
            SwitchRow(
                title = pollenType.name,
                checkedSummary = stringResource(resource = Res.string.pollen_alerts_on),
                uncheckedSummary = stringResource(resource = Res.string.pollen_alerts_off),
                isChecked = pollenType.pollenId in state.chosenPollenTypeIds,
                onCheckedChange = { isChecked: Boolean ->
                    onEvent(
                        PushAlertsEvent.OnPollenTypeToggled(
                            pollenTypeId = pollenType.pollenId,
                            isEnabled = isChecked
                        )
                    )
                }
            )
        }
        if (state.chosenPollenTypeIds.isNotEmpty()) {
            SettingsRowDivider()
            SettingsValueRow(
                title = stringResource(resource = Res.string.lowest_pollen_level),
                value = stringResource(resource = state.pollenLevel.labelResource()),
                trailingIconResource = Res.drawable.ic_chevron_right,
                onClick = { onEvent(PushAlertsEvent.OnPollenLevelRowClicked) }
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
            title = stringResource(resource = Res.string.lowest_warning_level),
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
        ExplanationDialog(
            titleResource = Res.string.notifications_are_off,
            textResource = Res.string.notifications_are_off_text,
            onDismiss = { onEvent(PushAlertsEvent.OnNotificationPermissionDialogDismissed) },
            modifier = modifier
        )
    }
    if (state.isOutsideSwedenDialogVisible) {
        ExplanationDialog(
            titleResource = Res.string.push_alerts_only_in_sweden,
            textResource = Res.string.push_alerts_only_in_sweden_text,
            onDismiss = { onEvent(PushAlertsEvent.OnOutsideSwedenDialogDismissed) },
            modifier = modifier
        )
    }
    if (state.isAttestationRefusedDialogVisible) {
        ExplanationDialog(
            titleResource = Res.string.push_alerts_not_available,
            textResource = Res.string.attestation_refused,
            onDismiss = { onEvent(PushAlertsEvent.OnAttestationRefusedDialogDismissed) },
            modifier = modifier
        )
    }
}

@Composable
private fun ExplanationDialog(
    titleResource: StringResource,
    textResource: StringResource,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(resource = Res.string.ok))
            }
        },
        title = { Text(text = stringResource(resource = titleResource)) },
        text = { Text(text = stringResource(resource = textResource)) }
    )
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
            modifier = Modifier.background(color = VadretTheme.colorScheme.background)
        )
    }
}