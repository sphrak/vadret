package se.vadretapp.vadret.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.onResult
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.AddFavoriteTask
import se.vadretapp.vadret.followedplace.ObserveLocationChoicesTask
import se.vadretapp.vadret.followedplace.RemoveFavoriteTask
import se.vadretapp.vadret.followedplace.SearchPlacesTask
import se.vadretapp.vadret.followedplace.SelectFavoriteTask
import se.vadretapp.vadret.followedplace.SetLocationModeTask
import se.vadretapp.vadret.followedplace.model.LocationChoices
import se.vadretapp.vadret.place.model.Place
import se.vadretapp.vadret.settings.extension.toPlacesFailureDialog
import se.vadretapp.vadret.ui.extension.launchReplacing
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val searchDebounce: Duration = 70.milliseconds

class PlacesViewModel internal constructor(
    private val observeLocationChoicesTask: ObserveLocationChoicesTask,
    private val setLocationModeTask: SetLocationModeTask,
    private val selectFavoriteTask: SelectFavoriteTask,
    private val removeFavoriteTask: RemoveFavoriteTask,
    private val searchPlacesTask: SearchPlacesTask,
    private val addFavoriteTask: AddFavoriteTask,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<PlacesState>
        field = MutableStateFlow(value = PlacesState())

    private val isStarted: AtomicBoolean = AtomicBoolean(value = false)
    private val searchJob: AtomicReference<Job?> = AtomicReference(value = null)

    internal fun onEvent(event: PlacesEvent) {
        when (event) {
            PlacesEvent.OnStart -> onStart()
            is PlacesEvent.OnFollowPositionToggled -> onFollowPositionToggled(isFollowingPosition = event.isFollowingPosition)
            is PlacesEvent.OnLocationPermissionResult -> onLocationPermissionResult(isGranted = event.isGranted)
            is PlacesEvent.OnSearchQueryChanged -> onSearchQueryChanged(query = event.query)
            is PlacesEvent.OnSearchSubmitted -> onSearchSubmitted(query = event.query)
            PlacesEvent.OnSearchCleared -> clearSearch()
            is PlacesEvent.OnSuggestionClicked -> onSuggestionClicked(place = event.place)
            is PlacesEvent.OnPlaceChosen -> onPlaceChosen(favorite = event.favorite)
            is PlacesEvent.OnPlaceRemoved -> onPlaceRemoved(favoriteId = event.favoriteId)
            PlacesEvent.OnFailureDismissed -> state.update { state: PlacesState -> state.copy(failureDialog = null) }
        }
    }

    private fun onStart() {
        if (isStarted.compareAndSet(expectedValue = false, newValue = true)) {
            viewModelScope.launch(context = coroutineDispatcher) { observeLocationChoices() }
        }
    }

    private suspend fun observeLocationChoices() {
        observeLocationChoicesTask().collect { result: Either<Failure, LocationChoices> ->
            result.onResult(
                onError = { failure: Failure -> displayFailure(failure = failure) },
                onSuccess = { locationChoices: LocationChoices ->
                    state.update { state: PlacesState ->
                        state.copy(
                            isFollowingPosition = locationChoices.isAutomatic,
                            favoriteChoices = locationChoices.favorites
                        )
                    }
                }
            )
        }
    }

    // The position is only followed once the app may read it.
    private fun onFollowPositionToggled(isFollowingPosition: Boolean) {
        if (isFollowingPosition) {
            state.update { state: PlacesState -> state.copy(isLocationPermissionRequested = true) }
        } else {
            setLocationMode(isAutomatic = false)
        }
    }

    private fun onLocationPermissionResult(isGranted: Boolean) {
        state.update { state: PlacesState -> state.copy(isLocationPermissionRequested = false) }
        if (isGranted) {
            setLocationMode(isAutomatic = true)
        } else {
            displayFailure(failure = Failure(errorCode = ErrorCode.LOCATION_PERMISSION_MISSING))
        }
    }

    private fun setLocationMode(isAutomatic: Boolean) {
        viewModelScope.launch(context = coroutineDispatcher) {
            setLocationModeTask(isAutomatic = isAutomatic).onResult(
                onError = { failure: Failure -> displayFailure(failure = failure) },
                onSuccess = {}
            )
        }
    }

    private fun onSearchQueryChanged(query: String) {
        state.update { state: PlacesState -> state.copy(searchQuery = query) }
        viewModelScope.launchReplacing(
            currentJob = searchJob,
            context = coroutineDispatcher
        ) {
            delay(duration = searchDebounce)
            searchPlacesTask(query = query).onResult(
                onError = { failure: Failure -> displayFailure(failure = failure) },
                onSuccess = { places: List<Place> -> displaySuggestions(query = query, suggestions = places) }
            )
        }
    }

    private fun displaySuggestions(
        query: String,
        suggestions: List<Place>
    ) {
        state.update { state: PlacesState ->
            if (state.searchQuery == query) {
                state.copy(suggestions = suggestions.toImmutableList())
            } else {
                state
            }
        }
    }

    // The shown places may still be for an earlier query, so the query as written is searched again.
    private fun onSearchSubmitted(query: String) {
        searchJob.exchange(newValue = null)?.cancel()
        viewModelScope.launch(context = coroutineDispatcher) {
            searchPlacesTask(query = query).onResult(
                onError = { failure: Failure -> displayFailure(failure = failure) },
                onSuccess = { places: List<Place> ->
                    val place: Place? = places.firstOrNull()
                    if (place != null) {
                        onSuggestionClicked(place = place)
                    }
                }
            )
        }
    }

    private fun clearSearch() {
        searchJob.exchange(newValue = null)?.cancel()
        state.update { state: PlacesState ->
            state.copy(
                searchQuery = "",
                suggestions = persistentListOf()
            )
        }
    }

    // A place found in the search is saved next to the others and the chosen place stays chosen. A saved place
    // of the same name is that place, so nothing more is saved.
    private fun onSuggestionClicked(place: Place) {
        clearSearch()
        viewModelScope.launch(context = coroutineDispatcher) {
            addFavoriteTask(
                locality = place.locality,
                municipality = place.municipality,
                county = place.county,
                latitude = place.latitude,
                longitude = place.longitude
            ).onResult(
                onError = { failure: Failure ->
                    if (failure.errorCode != ErrorCode.FAVORITE_NAME_TAKEN) {
                        displayFailure(failure = failure)
                    }
                },
                onSuccess = {}
            )
        }
    }

    private fun onPlaceChosen(favorite: Favorite) {
        viewModelScope.launch(context = coroutineDispatcher) {
            selectFavoriteTask(favorite = favorite).onResult(
                onError = { failure: Failure -> displayFailure(failure = failure) },
                onSuccess = {}
            )
        }
    }

    private fun onPlaceRemoved(favoriteId: String) {
        viewModelScope.launch(context = coroutineDispatcher) {
            removeFavoriteTask(favoriteId = favoriteId).onResult(
                onError = { failure: Failure -> displayFailure(failure = failure) },
                onSuccess = {}
            )
        }
    }

    private fun displayFailure(failure: Failure) {
        state.update { state: PlacesState -> state.copy(failureDialog = failure.toPlacesFailureDialog()) }
    }
}