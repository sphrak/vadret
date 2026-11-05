package se.vadretapp.vadret.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.vadretapp.vadret.common.Failure
import kotlin.concurrent.atomics.AtomicBoolean

class DeveloperViewModel internal constructor(
    private val getSettingsTask: GetSettingsTask,
    private val setExpandOnceTask: SetExpandOnceTask,
    private val setEndOfSeasonSimulatedTask: SetEndOfSeasonSimulatedTask,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<DeveloperState>
        field = MutableStateFlow(value = DeveloperState())

    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)

    internal fun onEvent(event: DeveloperEvent) {
        when (event) {
            DeveloperEvent.OnStart -> onStart()
            is DeveloperEvent.OnExpandOnceToggled -> onExpandOnceToggled(isExpandOnceEnabled = event.isExpandOnceEnabled)
            is DeveloperEvent.OnEndOfSeasonSimulationToggled -> onEndOfSeasonSimulationToggled(isEndOfSeasonSimulated = event.isEndOfSeasonSimulated)
        }
    }

    private fun onStart() {
        if (isStarted.compareAndSet(expectedValue = false, newValue = true)) {
            viewModelScope.launch(context = coroutineDispatcher) { loadSettings() }
        }
    }

    private suspend fun loadSettings() {
        getSettingsTask().onResult(
            onError = { _: Failure -> },
            onSuccess = { settings: Settings -> state.update { state: DeveloperState -> state.copy(settings = settings) } }
        )
    }

    private fun onExpandOnceToggled(isExpandOnceEnabled: Boolean) {
        viewModelScope.launch(context = coroutineDispatcher) {
            setExpandOnceTask(isExpandOnceEnabled = isExpandOnceEnabled).onResult(
                onError = { _: Failure -> },
                onSuccess = { loadSettings() }
            )
        }
    }

    private fun onEndOfSeasonSimulationToggled(isEndOfSeasonSimulated: Boolean) {
        viewModelScope.launch(context = coroutineDispatcher) {
            setEndOfSeasonSimulatedTask(isEndOfSeasonSimulated = isEndOfSeasonSimulated).onResult(
                onError = { _: Failure -> },
                onSuccess = { loadSettings() }
            )
        }
    }
}