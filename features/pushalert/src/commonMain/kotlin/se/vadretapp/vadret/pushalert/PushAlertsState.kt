package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertPlace

internal data class PushAlertsState(
    val isLoading: Boolean = true,
    val choices: PushAlertChoices = PushAlertChoices.NOTHING_CHOSEN,
    val places: List<PushAlertPlace> = emptyList(),
    val hasPollenAllergens: Boolean = false,
    val pendingChoices: PushAlertChoices? = null,
    val isWarningLevelDialogVisible: Boolean = false,
    val isPollenLevelDialogVisible: Boolean = false,
    val isHourFormatDialogVisible: Boolean = false,
    val isNoAllergensDialogVisible: Boolean = false,
    val isLastPlaceDialogVisible: Boolean = false,
    val isNotificationPermissionDialogVisible: Boolean = false,
    val isOutsideSwedenDialogVisible: Boolean = false,
    val isAttestationRefusedDialogVisible: Boolean = false,
    val isSyncPending: Boolean = false,
    val isSendFailedDialogVisible: Boolean = false,
    val failure: Failure? = null
)