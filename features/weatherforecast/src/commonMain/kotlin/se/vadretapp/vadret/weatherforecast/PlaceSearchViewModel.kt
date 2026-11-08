package se.vadretapp.vadret.weatherforecast

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.extension.onResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.SearchPlacesTask
import se.vadretapp.vadret.followedplace.SelectPlaceTask
import se.vadretapp.vadret.place.model.Place
import se.vadretapp.vadret.ui.extension.launchReplacing
import se.vadretapp.vadret.ui.extension.messageResource
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val searchDebounce: Duration = 70.milliseconds

// The forecast loads the chosen place when it is shown again, so the search only chooses the place and closes.
@Immutable
class PlaceSearchViewModel internal constructor(
    private val searchPlacesTask: SearchPlacesTask,
    private val selectPlaceTask: SelectPlaceTask,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<PlaceSearchState>
        field = MutableStateFlow(value = PlaceSearchState())

    private val searchJob: AtomicReference<Job?> = AtomicReference(value = null)

    internal fun onEvent(event: PlaceSearchEvent) {
        when (event) {
            is PlaceSearchEvent.OnQueryChanged -> onQueryChanged(query = event.query)
            is PlaceSearchEvent.OnSearchSubmitted -> onSearchSubmitted(query = event.query)
            is PlaceSearchEvent.OnSuggestionClicked -> onSuggestionClicked(place = event.place)
            PlaceSearchEvent.OnErrorShown -> state.update { state: PlaceSearchState ->
                state.copy(errorMessageResource = null)
            }
        }
    }

    private fun onQueryChanged(query: String) {
        state.update { state: PlaceSearchState ->
            state.copy(query = query)
        }
        viewModelScope.launchReplacing(
            currentJob = searchJob,
            context = coroutineDispatcher
        ) {
            delay(duration = searchDebounce)
            searchPlacesTask(query = query).onResult(
                onError = { failure: Failure -> displayError(failure = failure) },
                onSuccess = { suggestions: List<Place> ->
                    displaySuggestions(
                        query = query,
                        suggestions = suggestions.toImmutableList()
                    )
                }
            )
        }
    }

    private fun displaySuggestions(
        query: String,
        suggestions: ImmutableList<Place>
    ) {
        state.update { state: PlaceSearchState ->
            if (state.query == query) {
                state.copy(suggestions = suggestions)
            } else {
                state
            }
        }
    }

    // The places shown may still be for an earlier query, so the query as written is searched again.
    private fun onSearchSubmitted(query: String) {
        searchJob.exchange(newValue = null)?.cancel()
        viewModelScope.launch(context = coroutineDispatcher) {
            searchPlacesTask(query = query).onResult(
                onError = { failure: Failure -> displayError(failure = failure) },
                onSuccess = { places: List<Place> ->
                    val place: Place? = places.firstOrNull()
                    if (place != null) {
                        choose(place = place)
                    }
                }
            )
        }
    }

    private fun onSuggestionClicked(place: Place) {
        searchJob.exchange(newValue = null)?.cancel()
        viewModelScope.launch(context = coroutineDispatcher) {
            choose(place = place)
        }
    }

    private suspend fun choose(place: Place) {
        selectPlaceTask(place = place).onResult(
            onError = { failure: Failure -> displayError(failure = failure) },
            onSuccess = {
                state.update { state: PlaceSearchState ->
                    state.copy(isPlaceChosen = true)
                }
            }
        )
    }

    private fun displayError(failure: Failure) {
        state.update { state: PlaceSearchState ->
            state.copy(errorMessageResource = failure.messageResource())
        }
    }
}