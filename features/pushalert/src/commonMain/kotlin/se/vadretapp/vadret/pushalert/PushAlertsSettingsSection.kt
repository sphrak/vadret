package se.vadretapp.vadret.pushalert

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import se.vadretapp.vadret.pushalert.ui.PushAlertsRoute
import se.vadretapp.vadret.ui.SettingsSection
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.ic_notifications
import se.vadretapp.vadret.ui.resources.settings_notifications

internal class PushAlertsSettingsSection : SettingsSection {
    override val key: String = PUSH_ALERTS_SECTION_KEY
    override val titleResource: StringResource = Res.string.settings_notifications
    override val iconResource: DrawableResource = Res.drawable.ic_notifications

    @Composable
    override fun Content(
        onBack: () -> Unit,
        modifier: Modifier
    ) {
        PushAlertsRoute(
            onBack = onBack,
            modifier = modifier
        )
    }
}

private const val PUSH_ALERTS_SECTION_KEY = "push-alerts"