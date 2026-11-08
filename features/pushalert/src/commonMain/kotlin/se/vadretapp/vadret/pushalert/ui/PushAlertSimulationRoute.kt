package se.vadretapp.vadret.pushalert.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import se.vadretapp.vadret.di.koinViewModel
import se.vadretapp.vadret.pushalert.PushAlertSimulationEvent
import se.vadretapp.vadret.pushalert.PushAlertSimulationState
import se.vadretapp.vadret.pushalert.PushAlertSimulationViewModel
import se.vadretapp.vadret.pushalert.extension.icon
import se.vadretapp.vadret.pushalert.model.PushAlertMessage
import se.vadretapp.vadret.pushalert.model.PushAlertType
import se.vadretapp.vadret.pushalert.simulatedPushAlertMessages
import se.vadretapp.vadret.theme.model.Theme
import se.vadretapp.vadret.ui.InfoDialog
import se.vadretapp.vadret.ui.SettingsRow
import se.vadretapp.vadret.ui.SettingsRowIcon
import se.vadretapp.vadret.ui.SettingsScaffold
import se.vadretapp.vadret.ui.SettingsSectionHeader
import se.vadretapp.vadret.ui.ThemePreviewParameterProvider
import se.vadretapp.vadret.ui.VadretTheme
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.notifications_are_off
import se.vadretapp.vadret.ui.resources.notifications_are_off_text
import se.vadretapp.vadret.ui.resources.settings_notifications
import se.vadretapp.vadret.ui.resources.simulate_notifications
import kotlin.time.Instant

private const val SIMULATE_HEADER_KEY = "simulate-header"

private val previewNow: Instant = Instant.parse(input = "2026-11-08T18:00:00Z")

private enum class PushAlertSimulationContentType {
    HEADER,
    ROW
}

@Composable
internal fun PushAlertSimulationRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PushAlertSimulationViewModel = koinViewModel()
) {
    val state: PushAlertSimulationState by viewModel.state.collectAsStateWithLifecycle()
    PushAlertSimulationScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun PushAlertSimulationScreen(
    state: PushAlertSimulationState,
    onEvent: (PushAlertSimulationEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScaffold(
        title = stringResource(resource = Res.string.settings_notifications),
        onBack = onBack,
        modifier = modifier
    ) {
        item(key = SIMULATE_HEADER_KEY, contentType = PushAlertSimulationContentType.HEADER) {
            SettingsSectionHeader(text = stringResource(resource = Res.string.simulate_notifications))
        }
        items(
            items = state.pushAlertMessages,
            key = { pushAlertMessage: PushAlertMessage -> pushAlertMessage.title },
            contentType = { PushAlertSimulationContentType.ROW }
        ) { pushAlertMessage: PushAlertMessage ->
            SettingsRow(
                title = pushAlertMessage.title,
                onClick = { onEvent(PushAlertSimulationEvent.OnPushAlertClicked(pushAlertMessage = pushAlertMessage)) },
                leadingContent = pushAlertIcon(pushAlertType = pushAlertMessage.pushAlertType),
                summary = pushAlertMessage.body
            )
        }
    }
    if (state.isNotificationsOffDialogVisible) {
        InfoDialog(
            title = stringResource(resource = Res.string.notifications_are_off),
            text = stringResource(resource = Res.string.notifications_are_off_text),
            onDismiss = { onEvent(PushAlertSimulationEvent.OnNotificationsOffDismissed) }
        )
    }
}

private fun pushAlertIcon(pushAlertType: PushAlertType?): (@Composable () -> Unit)? =
    if (pushAlertType != null) {
        { SettingsRowIcon(icon = pushAlertType.icon()) }
    } else {
        null
    }

@Preview(device = Devices.PHONE)
@Composable
private fun PushAlertSimulationScreenPreview(
    @PreviewParameter(provider = ThemePreviewParameterProvider::class) theme: Theme
) {
    VadretTheme(theme = theme) {
        PushAlertSimulationScreen(
            state = PushAlertSimulationState(pushAlertMessages = simulatedPushAlertMessages(now = previewNow)),
            onEvent = {},
            onBack = {},
            modifier = Modifier.background(color = VadretTheme.colorScheme.background)
        )
    }
}