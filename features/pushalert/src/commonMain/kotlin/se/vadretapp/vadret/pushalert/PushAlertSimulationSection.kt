package se.vadretapp.vadret.pushalert

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.pushalert.ui.PushAlertSimulationRoute
import se.vadretapp.vadret.ui.DeveloperSettingsSection
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.settings_notifications

internal class PushAlertSimulationSection : DeveloperSettingsSection {
    override val key: String = PUSH_ALERT_SIMULATION_SECTION_KEY
    override val titleResource: StringResource = Res.string.settings_notifications

    @Composable
    override fun Content(
        onBack: () -> Unit,
        modifier: Modifier
    ) {
        PushAlertSimulationRoute(
            onBack = onBack,
            modifier = modifier
        )
    }
}

private const val PUSH_ALERT_SIMULATION_SECTION_KEY = "push-alert-simulation"