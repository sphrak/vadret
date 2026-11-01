package fi.kroon.vadret.pushalert

import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.pollen.model.PollenType
import fi.kroon.vadret.pushalert.model.PollenAlertLevel
import fi.kroon.vadret.pushalert.model.PushAlertChoices

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