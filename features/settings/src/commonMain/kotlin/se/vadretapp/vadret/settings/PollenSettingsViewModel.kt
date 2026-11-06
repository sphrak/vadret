package se.vadretapp.vadret.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.pollen.model.PollenType
import se.vadretapp.vadret.pollenseason.ObservePollenInSeasonTask
import se.vadretapp.vadret.settings.extension.toPollenGroupRows
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.concurrent.atomics.update

class PollenSettingsViewModel internal constructor(
    private val getSettingsTask: GetSettingsTask,
    private val setPollenEnabledTask: SetPollenEnabledTask,
    private val setPollenAllergenTask: SetPollenAllergenTask,
    private val observePollenTypesTask: ObservePollenTypesTask,
    private val observePollenInSeasonTask: ObservePollenInSeasonTask,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<PollenSettingsState>
        field = MutableStateFlow(value = PollenSettingsState())

    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)
    private val pollenTypes: AtomicReference<List<PollenType>> = AtomicReference(value = emptyList())
    private val allergenPollenIds: AtomicReference<Set<String>> = AtomicReference(value = emptySet())
    private val writeMutex: Mutex = Mutex()
    private val pendingWrites: AtomicInt = AtomicInt(value = 0)

    internal fun onEvent(event: PollenSettingsEvent) {
        when (event) {
            PollenSettingsEvent.OnStart -> onStart()
            is PollenSettingsEvent.OnPollenToggled -> onPollenToggled(isPollenEnabled = event.isPollenEnabled)
            is PollenSettingsEvent.OnPollenMarked -> onPollenMarked(
                pollenId = event.pollenId,
                isMarked = event.isMarked
            )
        }
    }

    private fun onStart() {
        if (isStarted.compareAndSet(expectedValue = false, newValue = true)) {
            viewModelScope.launch(context = coroutineDispatcher) {
                loadSettings()
                observePollenTypes()
            }
            viewModelScope.launch(context = coroutineDispatcher) { loadSeason() }
        }
    }

    private suspend fun loadSettings() {
        getSettingsTask().onResult(
            onError = { _: Failure -> },
            onSuccess = { settings: Settings ->
                allergenPollenIds.store(newValue = settings.allergenPollenIds)
                state.update { state: PollenSettingsState ->
                    state.copy(
                        isPollenEnabled = settings.isPollenEnabled,
                        pollenGroupRows = pollenTypes.load().toPollenGroupRows(allergenPollenIds = settings.allergenPollenIds)
                    )
                }
            }
        )
    }

    private suspend fun observePollenTypes() {
        observePollenTypesTask().collect { result: Either<Failure, List<PollenType>> ->
            result.onResult(
                onError = { _: Failure ->
                    state.update { state: PollenSettingsState ->
                        state.copy(
                            isLoading = false,
                            arePollenTypesUnavailable = true
                        )
                    }
                },
                onSuccess = { loadedPollenTypes: List<PollenType> ->
                    pollenTypes.store(newValue = loadedPollenTypes)
                    state.update { state: PollenSettingsState ->
                        state.copy(
                            isLoading = false,
                            pollenGroupRows = loadedPollenTypes.toPollenGroupRows(allergenPollenIds = allergenPollenIds.load()),
                            arePollenTypesUnavailable = false
                        )
                    }
                }
            )
        }
    }

    // Only a season known to be over is told, so a forecast that cannot be read says nothing about it.
    private suspend fun loadSeason() {
        observePollenInSeasonTask().first().onResult(
            onError = { _: Failure -> },
            onSuccess = { isInSeason: Boolean ->
                state.update { state: PollenSettingsState -> state.copy(isOffSeason = !isInSeason) }
            }
        )
    }

    private fun onPollenToggled(isPollenEnabled: Boolean) {
        store { setPollenEnabledTask(isPollenEnabled = isPollenEnabled) }
    }

    // A chip turns at once, so a second quick tap turns it back.
    private fun onPollenMarked(
        pollenId: String,
        isMarked: Boolean
    ) {
        allergenPollenIds.update { pollenIds: Set<String> ->
            if (isMarked) {
                pollenIds + pollenId
            } else {
                pollenIds - pollenId
            }
        }
        viewModelScope.launch(context = coroutineDispatcher) { showMarkedPollen() }
        store {
            setPollenAllergenTask(
                pollenId = pollenId,
                isAllergen = isMarked
            )
        }
    }

    // The rows are built from the marks as they are when this runs, so the last of quick taps is what is shown.
    private fun showMarkedPollen() {
        state.update { state: PollenSettingsState ->
            state.copy(pollenGroupRows = pollenTypes.load().toPollenGroupRows(allergenPollenIds = allergenPollenIds.load()))
        }
    }

    // Writes start in tap order and take turns, so the last tap is what is stored. What is stored is read back after the
    // last write, or at once when a write fails, so the screen never keeps a mark that was not stored.
    private fun store(write: suspend () -> Either<Failure, Unit>) {
        pendingWrites.incrementAndFetch()
        viewModelScope.launch(
            context = coroutineDispatcher,
            start = CoroutineStart.UNDISPATCHED
        ) {
            writeMutex.withLock {
                val result: Either<Failure, Unit> = write()
                val isLastWrite: Boolean = pendingWrites.decrementAndFetch() == 0
                if (result.isLeft || isLastWrite) {
                    loadSettings()
                }
            }
        }
    }
}