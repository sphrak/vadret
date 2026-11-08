package se.vadretapp.vadret.pushalert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.onResult
import io.github.sphrak.either.flatMap
import io.github.sphrak.either.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pushalert.extension.isNotified
import se.vadretapp.vadret.pushalert.extension.withNotified
import se.vadretapp.vadret.pushalert.model.PollenAlertLevel
import se.vadretapp.vadret.pushalert.model.PushAlertChoices
import se.vadretapp.vadret.pushalert.model.PushAlertPlace
import se.vadretapp.vadret.pushalert.model.WarningAlertLevel
import se.vadretapp.vadret.pushalert.usecase.IsOutsideSwedenTask
import se.vadretapp.vadret.pushalert.usecase.ObservePollenAllergensTask
import se.vadretapp.vadret.pushalert.usecase.ObservePushAlertChoicesTask
import se.vadretapp.vadret.pushalert.usecase.ObservePushAlertPlacesTask
import se.vadretapp.vadret.pushalert.usecase.ObservePushAlertSyncPendingTask
import se.vadretapp.vadret.pushalert.usecase.SavePushAlertChoicesTask
import se.vadretapp.vadret.pushalert.usecase.SyncPushAlertsTask
import kotlin.concurrent.atomics.AtomicBoolean

internal class PushAlertsViewModel(
    private val observePushAlertChoicesTask: ObservePushAlertChoicesTask,
    private val observePushAlertPlacesTask: ObservePushAlertPlacesTask,
    private val observePushAlertSyncPendingTask: ObservePushAlertSyncPendingTask,
    private val savePushAlertChoicesTask: SavePushAlertChoicesTask,
    private val syncPushAlertsTask: SyncPushAlertsTask,
    private val observePollenAllergensTask: ObservePollenAllergensTask,
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
            viewModelScope.launch(context = coroutineDispatcher) { load() }
            viewModelScope.launch(context = coroutineDispatcher) { observeSyncPending() }
            viewModelScope.launch(context = coroutineDispatcher) { observePollenAllergens() }
        }
    }

    private suspend fun load() {
        combine(
            flow = observePushAlertChoicesTask(),
            flow2 = observePushAlertPlacesTask()
        ) { pushAlertChoices: Either<Failure, PushAlertChoices>, pushAlertPlaces: Either<Failure, List<PushAlertPlace>> ->
            showChoices(
                pushAlertChoices = pushAlertChoices,
                pushAlertPlaces = pushAlertPlaces
            )
        }.collect()
    }

    private fun showChoices(
        pushAlertChoices: Either<Failure, PushAlertChoices>,
        pushAlertPlaces: Either<Failure, List<PushAlertPlace>>
    ) {
        pushAlertChoices
            .flatMap { choices: PushAlertChoices -> pushAlertPlaces.map { places: List<PushAlertPlace> -> choices to places } }
            .onResult(
                onError = { failure: Failure ->
                    state.update { state: PushAlertsState ->
                        state.copy(
                            isLoading = false,
                            failure = failure
                        )
                    }
                },
                onSuccess = { loaded: Pair<PushAlertChoices, List<PushAlertPlace>> ->
                    state.update { state: PushAlertsState ->
                        state.copy(
                            choices = loaded.first,
                            places = loaded.second,
                            isLoading = false
                        )
                    }
                }
            )
    }

    private suspend fun observeSyncPending() {
        observePushAlertSyncPendingTask().collect { result: Either<Failure, Boolean> ->
            result.onResult(
                onError = { failure: Failure -> state.update { state: PushAlertsState -> state.copy(failure = failure) } },
                onSuccess = { isSyncPending: Boolean -> state.update { state: PushAlertsState -> state.copy(isSyncPending = isSyncPending) } }
            )
        }
    }

    private suspend fun observePollenAllergens() {
        observePollenAllergensTask().collect { result: Either<Failure, Set<String>> ->
            result.onResult(
                onError = { failure: Failure -> state.update { state: PushAlertsState -> state.copy(failure = failure) } },
                onSuccess = { allergenPollenIds: Set<String> ->
                    state.update { state: PushAlertsState -> state.copy(hasPollenAllergens = allergenPollenIds.isNotEmpty()) }
                }
            )
        }
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
            is PushAlertsEvent.OnStrongWindToggled -> choose(pushAlertChoices = state.value.choices.copy(strongWind = event.isEnabled))
            is PushAlertsEvent.OnSlipperinessToggled -> choose(pushAlertChoices = state.value.choices.copy(slipperiness = event.isEnabled))
            is PushAlertsEvent.OnPlaceToggled -> onPlaceToggled(
                pushAlertPlace = event.pushAlertPlace,
                isEnabled = event.isEnabled
            )
            PushAlertsEvent.OnLastPlaceDialogDismissed -> state.update { state: PushAlertsState -> state.copy(isLastPlaceDialogVisible = false) }
            is PushAlertsEvent.OnPollenAlertsToggled -> onPollenAlertsToggled(isEnabled = event.isEnabled)
            PushAlertsEvent.OnNoAllergensDialogDismissed -> state.update { state: PushAlertsState -> state.copy(isNoAllergensDialogVisible = false) }
            PushAlertsEvent.OnPollenLevelRowClicked -> state.update { state: PushAlertsState -> state.copy(isPollenLevelDialogVisible = true) }
            PushAlertsEvent.OnPollenLevelDialogDismissed -> state.update { state: PushAlertsState -> state.copy(isPollenLevelDialogVisible = false) }
            is PushAlertsEvent.OnPollenLevelSelected -> {
                state.update { state: PushAlertsState -> state.copy(isPollenLevelDialogVisible = false) }
                choose(pushAlertChoices = state.value.choices.copy(pollenLowestLevel = event.pollenAlertLevel))
            }
            is PushAlertsEvent.OnNotificationPermissionResult -> onNotificationPermissionResult(isGranted = event.isGranted)
            PushAlertsEvent.OnNotificationPermissionDialogDismissed -> state.update { state: PushAlertsState ->
                state.copy(isNotificationPermissionDialogVisible = false)
            }
            PushAlertsEvent.OnOutsideSwedenDialogDismissed -> state.update { state: PushAlertsState -> state.copy(isOutsideSwedenDialogVisible = false) }
            PushAlertsEvent.OnAttestationRefusedDialogDismissed -> state.update { state: PushAlertsState ->
                state.copy(isAttestationRefusedDialogVisible = false)
            }
            PushAlertsEvent.OnFailureDismissed -> state.update { state: PushAlertsState -> state.copy(failure = null) }
            PushAlertsEvent.OnSendFailedDialogDismissed -> state.update { state: PushAlertsState -> state.copy(isSendFailedDialogVisible = false) }
        }
    }

    private suspend fun onPlaceToggled(
        pushAlertPlace: PushAlertPlace,
        isEnabled: Boolean
    ) {
        val currentState = state.value
        val pushAlertChoices = currentState.choices.withNotified(
            pushAlertPlace = pushAlertPlace,
            isNotified = isEnabled
        )
        if (currentState.places.none { place: PushAlertPlace -> pushAlertChoices.isNotified(pushAlertPlace = place) }) {
            state.update { state: PushAlertsState -> state.copy(isLastPlaceDialogVisible = true) }
        } else {
            choose(pushAlertChoices = pushAlertChoices)
        }
    }

    // Which pollen is pushed is the allergens picked for pollen, so without any there is nothing to turn on.
    private suspend fun onPollenAlertsToggled(isEnabled: Boolean) {
        if (isEnabled && !state.value.hasPollenAllergens) {
            state.update { state: PushAlertsState -> state.copy(isNoAllergensDialogVisible = true) }
        } else {
            choose(
                pushAlertChoices = state.value.choices.copy(
                    pollenLowestLevel = if (isEnabled) {
                        DEFAULT_POLLEN_LEVEL
                    } else {
                        null
                    }
                )
            )
        }
    }

    private suspend fun choose(pushAlertChoices: PushAlertChoices) {
        if (!pushAlertChoices.isWithin(other = state.value.choices) && isOutsideSwedenTask()) {
            state.update { state: PushAlertsState ->
                state.copy(isOutsideSwedenDialogVisible = true)
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
        state.update { state: PushAlertsState -> state.copy(choices = pushAlertChoices) }
        choicesToSave.trySend(element = pushAlertChoices)
    }

    private suspend fun saveAndSync(pushAlertChoices: PushAlertChoices) {
        savePushAlertChoicesTask(pushAlertChoices = pushAlertChoices).onResult(
            onError = { failure: Failure -> state.update { state: PushAlertsState -> state.copy(failure = failure) } },
            onSuccess = { sync() }
        )
    }

    // The person changed a choice, so a send that fails is told at once, while the notice that the choices are not sent
    // stays until a later send works.
    private suspend fun sync() {
        val synced = syncPushAlertsTask()
        val failure: Failure? = if (synced is Either.Left) {
            synced.a
        } else {
            null
        }
        val isAttestationRefused = failure?.errorCode == ErrorCode.ATTESTATION_REFUSED
        val isUnsupportedArea = failure?.errorCode == ErrorCode.UNSUPPORTED_AREA
        state.update { state: PushAlertsState ->
            state.copy(
                failure = null,
                isSendFailedDialogVisible = failure != null && !isAttestationRefused && !isUnsupportedArea,
                isOutsideSwedenDialogVisible = state.isOutsideSwedenDialogVisible || isUnsupportedArea,
                isAttestationRefusedDialogVisible = state.isAttestationRefusedDialogVisible || isAttestationRefused
            )
        }
    }
}

private val DEFAULT_WARNING_LEVEL = WarningAlertLevel.YELLOW
private val DEFAULT_POLLEN_LEVEL = PollenAlertLevel.MODERATE_TO_HIGH