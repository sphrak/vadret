package se.vadretapp.vadret.weatherforecast

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.AddFavoriteTask
import se.vadretapp.vadret.followedplace.SelectFavoriteTask
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.ui.extension.launchReplacing
import se.vadretapp.vadret.ui.extension.messageResource
import se.vadretapp.vadret.weatherforecast.extension.localityFor
import se.vadretapp.vadret.weatherforecast.extension.toCurrentWeather
import se.vadretapp.vadret.weatherforecast.extension.withNameTaken
import se.vadretapp.vadret.weatherforecast.extension.withoutName
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast
import se.vadretapp.vadret.weatherforecast.model.PlaceNaming
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.Clock

@Immutable
class PlaceIslandViewModel internal constructor(
    private val getPlaceForecastTask: GetPlaceForecastTask,
    private val addFavoriteTask: AddFavoriteTask,
    private val selectFavoriteTask: SelectFavoriteTask,
    private val clock: Clock,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    internal val state: StateFlow<PlaceIslandState>
        field = MutableStateFlow(value = PlaceIslandState())

    private val loadJob: AtomicReference<Job?> = AtomicReference(value = null)
    private val saveJob: AtomicReference<Job?> = AtomicReference(value = null)

    internal fun onEvent(event: PlaceIslandEvent) {
        when (event) {
            is PlaceIslandEvent.OnPlaceDropped -> onPlaceDropped(
                location = Location(
                    latitude = event.latitude,
                    longitude = event.longitude
                )
            )
            PlaceIslandEvent.OnRetryClicked -> onRetryClicked()
            is PlaceIslandEvent.OnLabelChanged -> state.update { state: PlaceIslandState ->
                state.copy(
                    placeNaming = state.placeNaming?.copy(
                        label = event.label,
                        isLabelMissing = false
                    )
                )
            }
            PlaceIslandEvent.OnSaveClicked -> onSaveClicked()
            PlaceIslandEvent.OnLabelDismissed -> state.update { state: PlaceIslandState ->
                state.copy(placeNaming = null)
            }
            PlaceIslandEvent.OnOpenForecastClicked -> onOpenForecastClicked()
            PlaceIslandEvent.OnOpeningShown -> onOpeningShown()
        }
    }

    // The radar shows the same drop again when it comes back into view, so the island keeps what it has.
    private fun onPlaceDropped(location: Location) {
        if (state.value.location == location) {
            return
        }
        saveJob.exchange(newValue = null)?.cancel()
        state.value = PlaceIslandState(location = location)
        load(location = location)
    }

    private fun onRetryClicked() {
        val location: Location = state.value.location ?: return
        load(location = location)
    }

    private fun load(location: Location) {
        state.update { state: PlaceIslandState ->
            state.copy(
                isLoading = true,
                errorMessageResource = null
            )
        }
        viewModelScope.launchReplacing(
            currentJob = loadJob,
            context = coroutineDispatcher
        ) {
            getPlaceForecastTask(
                latitude = location.latitude,
                longitude = location.longitude
            ).onResult(
                onError = { failure: Failure ->
                    updateFor(location = location) { state: PlaceIslandState ->
                        state.copy(
                            isLoading = false,
                            errorMessageResource = failure.messageResource()
                        )
                    }
                },
                onSuccess = { placeForecast: PlaceForecast ->
                    updateFor(location = location) { state: PlaceIslandState ->
                        state.copy(
                            placeName = placeForecast.placeName,
                            municipality = placeForecast.municipality,
                            county = placeForecast.county,
                            currentWeather = placeForecast.toCurrentWeather(now = clock.now()),
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    private fun onSaveClicked() {
        val shownState: PlaceIslandState = state.value
        val location: Location = shownState.location ?: return
        val placeNaming: PlaceNaming? = shownState.placeNaming
        val locality: String? = placeNaming.localityFor(placeName = shownState.placeName)
        if (locality == null) {
            state.update { state: PlaceIslandState -> state.copy(placeNaming = placeNaming.withoutName()) }
            return
        }
        viewModelScope.launchReplacing(
            currentJob = saveJob,
            context = coroutineDispatcher
        ) {
            addFavoriteTask(
                locality = locality,
                municipality = shownState.municipality.orEmpty(),
                county = shownState.county.orEmpty(),
                latitude = location.latitude,
                longitude = location.longitude
            ).onResult(
                onError = { failure: Failure ->
                    updateFor(location = location) { state: PlaceIslandState ->
                        if (failure.errorCode == ErrorCode.FAVORITE_NAME_TAKEN) {
                            state.copy(placeNaming = state.placeNaming.withNameTaken(placeName = locality))
                        } else {
                            state.copy(errorMessageResource = failure.messageResource())
                        }
                    }
                },
                onSuccess = { favorite: Favorite ->
                    updateFor(location = location) { state: PlaceIslandState ->
                        state.copy(
                            savedFavorite = favorite,
                            placeNaming = null,
                            errorMessageResource = null
                        )
                    }
                }
            )
        }
    }

    // Chosen before the weather tab opens, so the tab loads the new place.
    private fun onOpenForecastClicked() {
        val shownState: PlaceIslandState = state.value
        val location: Location = shownState.location ?: return
        val savedFavorite: Favorite? = shownState.savedFavorite
        if (savedFavorite == null) {
            state.update { state: PlaceIslandState -> state.copy(opening = PlaceOpening.PLACE_FORECAST) }
            return
        }
        viewModelScope.launchReplacing(
            currentJob = saveJob,
            context = coroutineDispatcher
        ) {
            selectFavoriteTask(favorite = savedFavorite).onResult(
                onError = { failure: Failure ->
                    updateFor(location = location) { state: PlaceIslandState ->
                        state.copy(errorMessageResource = failure.messageResource())
                    }
                },
                onSuccess = {
                    updateFor(location = location) { state: PlaceIslandState ->
                        state.copy(opening = PlaceOpening.CHOSEN_LOCATION)
                    }
                }
            )
        }
    }

    // The radar forgets the drop too, so dropping on the same spot later loads it again.
    private fun onOpeningShown() {
        if (state.value.opening == PlaceOpening.CHOSEN_LOCATION) {
            loadJob.exchange(newValue = null)?.cancel()
            state.value = PlaceIslandState()
        } else {
            state.update { state: PlaceIslandState -> state.copy(opening = null) }
        }
    }

    // A place dropped while an earlier one still loads or saves must not get that place's answer.
    private fun updateFor(
        location: Location,
        transform: (PlaceIslandState) -> PlaceIslandState
    ) {
        state.update { state: PlaceIslandState ->
            if (state.location == location) {
                transform(state)
            } else {
                state
            }
        }
    }
}