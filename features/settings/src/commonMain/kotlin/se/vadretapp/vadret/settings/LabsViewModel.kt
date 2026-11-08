package se.vadretapp.vadret.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.settings.model.Labs
import se.vadretapp.vadret.settings.usecase.ObserveLabsTask
import se.vadretapp.vadret.settings.usecase.SetAuroraTask
import se.vadretapp.vadret.settings.usecase.SetHourlyForecastTask
import kotlin.concurrent.atomics.AtomicBoolean

class LabsViewModel internal constructor(
    private val observeLabsTask: ObserveLabsTask,
    private val setHourlyForecastTask: SetHourlyForecastTask,
    private val setAuroraTask: SetAuroraTask,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<LabsState>
        field = MutableStateFlow(value = LabsState())

    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)
    private val writeMutex: Mutex = Mutex()

    internal fun onEvent(event: LabsEvent) {
        when (event) {
            LabsEvent.OnStart -> onStart()
            is LabsEvent.OnHourlyForecastToggled -> store { setHourlyForecastTask(isHourlyForecastEnabled = event.isHourlyForecastEnabled) }
            is LabsEvent.OnAuroraToggled -> store { setAuroraTask(isAuroraEnabled = event.isAuroraEnabled) }
            LabsEvent.OnFailureDismissed -> state.update { state: LabsState -> state.copy(failure = null) }
        }
    }

    private fun onStart() {
        if (isStarted.compareAndSet(expectedValue = false, newValue = true)) {
            viewModelScope.launch(context = coroutineDispatcher) { observeLabs() }
        }
    }

    // The switches show what is stored, so a toggle that cannot be stored leaves its switch as it was.
    private suspend fun observeLabs() {
        observeLabsTask().collect { labs: Either<Failure, Labs> ->
            labs.onResult(
                onError = { failure: Failure -> showFailure(failure = failure) },
                onSuccess = { storedLabs: Labs ->
                    state.update { state: LabsState -> state.copy(labs = storedLabs) }
                }
            )
        }
    }

    // Writes start in tap order and take turns, so the last tap is what is stored.
    private fun store(write: suspend () -> Either<Failure, Unit>) {
        viewModelScope.launch(
            context = coroutineDispatcher,
            start = CoroutineStart.UNDISPATCHED
        ) {
            writeMutex.withLock {
                write().onResult(
                    onError = { failure: Failure -> showFailure(failure = failure) },
                    onSuccess = {}
                )
            }
        }
    }

    private fun showFailure(failure: Failure) {
        state.update { state: LabsState -> state.copy(failure = failure) }
    }
}