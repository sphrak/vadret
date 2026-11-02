package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PushAlertChoices

internal data class PushAlertsState(
    val choices: PushAlertChoices = PushAlertChoices.NOTHING_CHOSEN,
    val chosenPollenTypeIds: Set<String> = emptySet(),
    val pollenLevel: PollenAlertLevel = PollenAlertLevel.MODERATE_TO_HIGH,
    val pollenTypes: List<PollenType> = emptyList(),
    val pendingChoices: PushAlertChoices? = null,
    val isWarningLevelDialogVisible: Boolean = false,
    val isPollenLevelDialogVisible: Boolean = false,
    val isNotificationPermissionDialogVisible: Boolean = false,
    val isOutsideSwedenDialogVisible: Boolean = false,
    val isAttestationRefusedDialogVisible: Boolean = false,
    val isSyncPending: Boolean = false,
    val failure: Failure? = null
)