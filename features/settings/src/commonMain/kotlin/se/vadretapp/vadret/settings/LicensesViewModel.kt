package se.vadretapp.vadret.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.vadretapp.vadret.library.model.Library
import se.vadretapp.vadret.settings.extension.toLibraryGroups
import kotlin.concurrent.atomics.AtomicBoolean

class LicensesViewModel internal constructor(
    private val getLibrariesTask: GetLibrariesTask,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<LicensesState>
        field = MutableStateFlow(value = LicensesState())

    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)

    internal fun onEvent(event: LicensesEvent) {
        when (event) {
            LicensesEvent.OnStart -> onStart()
            is LicensesEvent.OnLibraryClicked -> state.update { state: LicensesState -> state.copy(selectedLibrary = event.library) }
            LicensesEvent.OnLibraryDismissed -> state.update { state: LicensesState -> state.copy(selectedLibrary = null) }
        }
    }

    private fun onStart() {
        if (isStarted.compareAndSet(expectedValue = false, newValue = true)) {
            viewModelScope.launch(context = coroutineDispatcher) { loadLibraries() }
        }
    }

    private fun loadLibraries() {
        getLibrariesTask().onResult(
            onError = { },
            onSuccess = { libraries: List<Library> ->
                state.update { state: LicensesState -> state.copy(libraryGroups = libraries.toLibraryGroups()) }
            }
        )
    }
}