package se.vadretapp.vadret.pushalert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.vadretapp.vadret.pushalert.model.PushAlertMessage
import se.vadretapp.vadret.pushalert.usecase.ShowSimulatedPushAlertTask
import kotlin.time.Clock

internal class PushAlertSimulationViewModel(
    private val showSimulatedPushAlertTask: ShowSimulatedPushAlertTask,
    clock: Clock,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<PushAlertSimulationState>
        field = MutableStateFlow(value = PushAlertSimulationState(pushAlertMessages = simulatedPushAlertMessages(now = clock.now())))

    internal fun onEvent(event: PushAlertSimulationEvent) {
        when (event) {
            is PushAlertSimulationEvent.OnPushAlertClicked -> onPushAlertClicked(pushAlertMessage = event.pushAlertMessage)
            PushAlertSimulationEvent.OnNotificationsOffDismissed -> state.update { state: PushAlertSimulationState -> state.copy(isNotificationsOffDialogVisible = false) }
        }
    }

    private fun onPushAlertClicked(pushAlertMessage: PushAlertMessage) {
        viewModelScope.launch(context = coroutineDispatcher) {
            if (!showSimulatedPushAlertTask(pushAlertMessage = pushAlertMessage)) {
                state.update { state: PushAlertSimulationState -> state.copy(isNotificationsOffDialogVisible = true) }
            }
        }
    }
}