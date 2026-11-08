package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PushAlertPlace
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel

internal sealed interface PushAlertsEvent {
    data object OnStart : PushAlertsEvent
    data class OnWarningAlertsToggled(val isEnabled: Boolean) : PushAlertsEvent
    data object OnWarningLevelRowClicked : PushAlertsEvent
    data object OnWarningLevelDialogDismissed : PushAlertsEvent
    data class OnWarningLevelSelected(val warningAlertLevel: WarningAlertLevel) : PushAlertsEvent
    data class OnTemperatureTrendToggled(val isEnabled: Boolean) : PushAlertsEvent
    data class OnStrongWindToggled(val isEnabled: Boolean) : PushAlertsEvent
    data class OnSlipperinessToggled(val isEnabled: Boolean) : PushAlertsEvent
    data class OnPollenAlertsToggled(val isEnabled: Boolean) : PushAlertsEvent
    data object OnNoAllergensDialogDismissed : PushAlertsEvent
    data class OnPlaceToggled(
        val pushAlertPlace: PushAlertPlace,
        val isEnabled: Boolean
    ) : PushAlertsEvent
    data object OnLastPlaceDialogDismissed : PushAlertsEvent
    data object OnPollenLevelRowClicked : PushAlertsEvent
    data object OnPollenLevelDialogDismissed : PushAlertsEvent
    data class OnPollenLevelSelected(val pollenAlertLevel: PollenAlertLevel) : PushAlertsEvent
    data class OnNotificationPermissionResult(val isGranted: Boolean) : PushAlertsEvent
    data object OnNotificationPermissionDialogDismissed : PushAlertsEvent
    data object OnOutsideSwedenDialogDismissed : PushAlertsEvent
    data object OnAttestationRefusedDialogDismissed : PushAlertsEvent
    data object OnFailureDismissed : PushAlertsEvent
    data object OnSendFailedDialogDismissed : PushAlertsEvent
}