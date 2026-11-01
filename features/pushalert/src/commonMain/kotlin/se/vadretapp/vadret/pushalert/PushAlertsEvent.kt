package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel

internal sealed interface PushAlertsEvent {
    data object OnStart : PushAlertsEvent
    data class OnWarningAlertsToggled(val isEnabled: Boolean) : PushAlertsEvent
    data object OnWarningLevelRowClicked : PushAlertsEvent
    data object OnWarningLevelDialogDismissed : PushAlertsEvent
    data class OnWarningLevelSelected(val warningAlertLevel: WarningAlertLevel) : PushAlertsEvent
    data class OnTemperatureTrendToggled(val isEnabled: Boolean) : PushAlertsEvent
    data class OnStrongWindToggled(val isEnabled: Boolean) : PushAlertsEvent
    data class OnPollenTypeToggled(
        val pollenTypeId: String,
        val isEnabled: Boolean
    ) : PushAlertsEvent
    data object OnPollenLevelRowClicked : PushAlertsEvent
    data object OnPollenLevelDialogDismissed : PushAlertsEvent
    data class OnPollenLevelSelected(val pollenAlertLevel: PollenAlertLevel) : PushAlertsEvent
    data class OnFavoriteToggled(
        val favoriteId: String,
        val isEnabled: Boolean
    ) : PushAlertsEvent
    data object OnFavoriteLimitDialogDismissed : PushAlertsEvent
    data class OnNotificationPermissionResult(val isGranted: Boolean) : PushAlertsEvent
    data object OnNotificationPermissionDialogDismissed : PushAlertsEvent
    data object OnOutsideSwedenDialogDismissed : PushAlertsEvent
    data object OnAttestationRefusedDialogDismissed : PushAlertsEvent
    data object OnFailureDismissed : PushAlertsEvent
}