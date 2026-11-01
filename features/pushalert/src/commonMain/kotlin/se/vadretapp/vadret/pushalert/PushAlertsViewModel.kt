package se.vadretapp.vadret.pushalert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PollenChoice
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel
import se.vadretapp.vadret.pushalert.usecase.GetPollenTypesTask
import se.vadretapp.vadret.pushalert.usecase.IsOutsideSwedenTask
import se.vadretapp.vadret.pushalert.usecase.ObservePushAlertChoicesTask
import se.vadretapp.vadret.pushalert.usecase.ObservePushAlertSyncPendingTask
import se.vadretapp.vadret.pushalert.usecase.SavePushAlertChoicesTask
import se.vadretapp.vadret.pushalert.usecase.SyncPushAlertsTask
import kotlin.concurrent.atomics.AtomicBoolean

internal class PushAlertsViewModel(
    private val observePushAlertChoicesTask: ObservePushAlertChoicesTask,
    private val observePushAlertSyncPendingTask: ObservePushAlertSyncPendingTask,
    private val savePushAlertChoicesTask: SavePushAlertChoicesTask,
    private val syncPushAlertsTask: SyncPushAlertsTask,
    private val getPollenTypesTask: GetPollenTypesTask,
    private val isOutsideSwedenTask: IsOutsideSwedenTask,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<PushAlertsState>
        field = MutableStateFlow(value = PushAlertsState())

    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)
    private val events: Channel<PushAlertsEvent> = Channel(capacity = Channel.UNLIMITED)
    private val choicesToSave: Channel<PushAlertChoices> = Channel(capacity = Channel.CONFLATED)

    internal fun onEvent(event: PushAlertsEvent) {
        if (event == PushAlertsEvent.OnStart) {
            onStart()
        } else {
            events.trySend(element = event)
        }
    }

    private fun onStart() {
        if (isStarted.compareAndSet(expectedValue = false, newValue = true)) {
            viewModelScope.launch(context = coroutineDispatcher) {
                for (event in events) {
                    handle(event = event)
                }
            }
            viewModelScope.launch(context = coroutineDispatcher) {
                for (pushAlertChoices in choicesToSave) {
                    saveAndSync(pushAlertChoices = pushAlertChoices)
                }
            }
            viewModelScope.launch(context = coroutineDispatcher) { observeChoices() }
            viewModelScope.launch(context = coroutineDispatcher) { observeSyncPending() }
            viewModelScope.launch(context = coroutineDispatcher) { loadPollenTypes() }
        }
    }

    private suspend fun observeChoices() {
        observePushAlertChoicesTask().collect { result: Either<Failure, PushAlertChoices> ->
            result.onResult(
                onError = { failure: Failure -> state.update { state: PushAlertsState -> state.copy(failure = failure) } },
                onSuccess = { pushAlertChoices: PushAlertChoices -> state.update { state: PushAlertsState -> state.withChoices(pushAlertChoices = pushAlertChoices) } }
            )
        }
    }

    private suspend fun observeSyncPending() {
        observePushAlertSyncPendingTask().collect { result: Either<Failure, Boolean> ->
            result.onResult(
                onError = { failure: Failure -> state.update { state: PushAlertsState -> state.copy(failure = failure) } },
                onSuccess = { isSyncPending: Boolean -> state.update { state: PushAlertsState -> state.copy(isSyncPending = isSyncPending) } }
            )
        }
    }

    private suspend fun loadPollenTypes() {
        getPollenTypesTask().onResult(
            onError = { failure: Failure -> state.update { state: PushAlertsState -> state.copy(failure = failure) } },
            onSuccess = { pollenTypes: List<PollenType> -> state.update { state: PushAlertsState -> state.copy(pollenTypes = pollenTypes) } }
        )
    }

    private suspend fun handle(event: PushAlertsEvent) {
        when (event) {
            PushAlertsEvent.OnStart -> Unit
            is PushAlertsEvent.OnWarningAlertsToggled -> choose(
                pushAlertChoices = state.value.choices.copy(
                    warningLowestLevel = if (event.isEnabled) {
                        DEFAULT_WARNING_LEVEL
                    } else {
                        null
                    }
                )
            )
            PushAlertsEvent.OnWarningLevelRowClicked -> state.update { state: PushAlertsState -> state.copy(isWarningLevelDialogVisible = true) }
            PushAlertsEvent.OnWarningLevelDialogDismissed -> state.update { state: PushAlertsState -> state.copy(isWarningLevelDialogVisible = false) }
            is PushAlertsEvent.OnWarningLevelSelected -> {
                state.update { state: PushAlertsState -> state.copy(isWarningLevelDialogVisible = false) }
                choose(pushAlertChoices = state.value.choices.copy(warningLowestLevel = event.warningAlertLevel))
            }
            is PushAlertsEvent.OnTemperatureTrendToggled -> choose(pushAlertChoices = state.value.choices.copy(temperatureTrend = event.isEnabled))
            is PushAlertsEvent.OnPollenTypeToggled -> onPollenTypeToggled(
                pollenTypeId = event.pollenTypeId,
                isEnabled = event.isEnabled
            )
            PushAlertsEvent.OnPollenLevelRowClicked -> state.update { state: PushAlertsState -> state.copy(isPollenLevelDialogVisible = true) }
            PushAlertsEvent.OnPollenLevelDialogDismissed -> state.update { state: PushAlertsState -> state.copy(isPollenLevelDialogVisible = false) }
            is PushAlertsEvent.OnPollenLevelSelected -> onPollenLevelSelected(pollenAlertLevel = event.pollenAlertLevel)
            is PushAlertsEvent.OnNotificationPermissionResult -> onNotificationPermissionResult(isGranted = event.isGranted)
            PushAlertsEvent.OnNotificationPermissionDialogDismissed -> state.update { state: PushAlertsState ->
                state.copy(isNotificationPermissionDialogVisible = false)
            }
            PushAlertsEvent.OnOutsideSwedenDialogDismissed -> state.update { state: PushAlertsState -> state.copy(isOutsideSwedenDialogVisible = false) }
            PushAlertsEvent.OnAttestationRefusedDialogDismissed -> state.update { state: PushAlertsState ->
                state.copy(isAttestationRefusedDialogVisible = false)
            }
            PushAlertsEvent.OnFailureDismissed -> state.update { state: PushAlertsState -> state.copy(failure = null) }
        }
    }

    private suspend fun onPollenTypeToggled(
        pollenTypeId: String,
        isEnabled: Boolean
    ) {
        val currentState = state.value
        val otherPollenChoices = currentState.choices.pollenChoices.filter { pollenChoice: PollenChoice -> pollenChoice.pollenTypeId != pollenTypeId }
        val pollenChoices = if (isEnabled) {
            otherPollenChoices + PollenChoice(
                pollenTypeId = pollenTypeId,
                lowestLevel = currentState.pollenLevel
            )
        } else {
            otherPollenChoices
        }
        choose(pushAlertChoices = currentState.choices.copy(pollenChoices = pollenChoices))
    }

    private suspend fun onPollenLevelSelected(pollenAlertLevel: PollenAlertLevel) {
        state.update { state: PushAlertsState ->
            state.copy(
                isPollenLevelDialogVisible = false,
                pollenLevel = pollenAlertLevel
            )
        }
        val choices = state.value.choices
        choose(
            pushAlertChoices = choices.copy(
                pollenChoices = choices.pollenChoices.map { pollenChoice: PollenChoice -> pollenChoice.copy(lowestLevel = pollenAlertLevel) }
            )
        )
    }

    private suspend fun choose(pushAlertChoices: PushAlertChoices) {
        if (!pushAlertChoices.isWithin(other = state.value.choices) && isOutsideSwedenTask()) {
            state.update { state: PushAlertsState ->
                state.withChoices(pushAlertChoices = state.choices).copy(isOutsideSwedenDialogVisible = true)
            }
        } else if (!state.value.choices.isAnythingChosen() && pushAlertChoices.isAnythingChosen()) {
            state.update { state: PushAlertsState -> state.copy(pendingChoices = pushAlertChoices) }
        } else {
            save(pushAlertChoices = pushAlertChoices)
        }
    }

    private fun onNotificationPermissionResult(isGranted: Boolean) {
        val pendingChoices = state.value.pendingChoices ?: return
        state.update { state: PushAlertsState ->
            state.copy(
                pendingChoices = null,
                isNotificationPermissionDialogVisible = !isGranted
            )
        }
        if (isGranted) {
            save(pushAlertChoices = pendingChoices)
        }
    }

    private fun save(pushAlertChoices: PushAlertChoices) {
        state.update { state: PushAlertsState -> state.withChoices(pushAlertChoices = pushAlertChoices) }
        choicesToSave.trySend(element = pushAlertChoices)
    }

    private suspend fun saveAndSync(pushAlertChoices: PushAlertChoices) {
        savePushAlertChoicesTask(pushAlertChoices = pushAlertChoices).onResult(
            onError = { failure: Failure -> state.update { state: PushAlertsState -> state.copy(failure = failure) } },
            onSuccess = { sync() }
        )
    }

    private suspend fun sync() {
        val synced = syncPushAlertsTask()
        val isAttestationRefused = synced is Either.Left && synced.a.errorCode == ErrorCode.ATTESTATION_REFUSED
        val isUnsupportedArea = synced is Either.Left && synced.a.errorCode == ErrorCode.UNSUPPORTED_AREA
        state.update { state: PushAlertsState ->
            state.copy(
                failure = null,
                isOutsideSwedenDialogVisible = state.isOutsideSwedenDialogVisible || isUnsupportedArea,
                isAttestationRefusedDialogVisible = state.isAttestationRefusedDialogVisible || isAttestationRefused
            )
        }
    }
}

private fun PushAlertsState.withChoices(pushAlertChoices: PushAlertChoices): PushAlertsState =
    copy(
        choices = pushAlertChoices,
        chosenPollenTypeIds = pushAlertChoices.pollenChoices.map { pollenChoice: PollenChoice -> pollenChoice.pollenTypeId }.toSet(),
        pollenLevel = pushAlertChoices.pollenChoices.firstOrNull()?.lowestLevel ?: pollenLevel
    )

private val DEFAULT_WARNING_LEVEL = WarningAlertLevel.YELLOW