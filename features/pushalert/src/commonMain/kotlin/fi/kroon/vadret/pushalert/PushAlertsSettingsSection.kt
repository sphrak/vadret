package fi.kroon.vadret.pushalert

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fi.kroon.vadret.pushalert.ui.PushAlertsRoute
import fi.kroon.vadret.ui.SettingsSection

internal class PushAlertsSettingsSection : SettingsSection {
    override val key: String = PUSH_ALERTS_SECTION_KEY

    @Composable
    override fun Content(modifier: Modifier) {
        PushAlertsRoute(modifier = modifier)
    }
}

private const val PUSH_ALERTS_SECTION_KEY = "push-alerts"