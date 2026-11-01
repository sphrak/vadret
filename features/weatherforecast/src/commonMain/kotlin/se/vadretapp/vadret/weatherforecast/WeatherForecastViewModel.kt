package se.vadretapp.vadret.weatherforecast

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.extension.errorOrNull
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
import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences
import se.vadretapp.vadret.place.model.Place
import se.vadretapp.vadret.ui.extension.launchReplacing
import se.vadretapp.vadret.ui.extension.messageResource
import se.vadretapp.vadret.ui.format.currentLanguageTag
import se.vadretapp.vadret.warning.ObserveUnseenWarningsTask
import se.vadretapp.vadret.warning.model.WarningPlace
import se.vadretapp.vadret.weatherforecast.extension.toLatestWarning
import se.vadretapp.vadret.weatherforecast.extension.toWeatherForecast
import se.vadretapp.vadret.weatherforecast.extension.withComingDays
import se.vadretapp.vadret.weatherforecast.extension.withDayToggled
import se.vadretapp.vadret.weatherforecast.extension.withForecast
import se.vadretapp.vadret.weatherforecast.model.ForecastView
import se.vadretapp.vadret.weatherforecast.model.LocationChoices
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast
import se.vadretapp.vadret.weatherforecast.model.SearchState
import se.vadretapp.vadret.weatherforecast.model.WeatherForecast
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val searchDebounce: Duration = 70.milliseconds
private val staleForecastAge: Duration = 2.minutes

@Immutable
class WeatherForecastViewModel internal constructor(
    private val getLocationModeTask: GetLocationModeTask,
    private val setLocationModeTask: SetLocationModeTask,
    private val getWeatherForecastTask: GetWeatherForecastTask,
    private val observeLocationChoicesTask: ObserveLocationChoicesTask,
    private val observeForecastPreferencesTask: ObserveForecastPreferencesTask,
    private val selectFavoriteTask: SelectFavoriteTask,
    private val removeFavoriteTask: RemoveFavoriteTask,
    private val searchPlacesTask: SearchPlacesTask,
    private val selectPlaceTask: SelectPlaceTask,
    private val observeUnseenWarningsTask: ObserveUnseenWarningsTask,
    private val clock: Clock,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    val state: StateFlow<WeatherForecastState>
        field = MutableStateFlow(value = WeatherForecastState())

    private val loadedLocationMode: AtomicReference<Boolean?> = AtomicReference(value = null)
    private val languageTag: AtomicReference<String> = AtomicReference(value = currentLanguageTag())
    private val placeForecast: AtomicReference<PlaceForecast?> = AtomicReference(value = null)
    private val unseenWarnings: AtomicReference<List<AggregatedFeed>> = AtomicReference(value = emptyList())
    private val fetchedAt: AtomicReference<Instant?> = AtomicReference(value = null)
    private val isObservingLocationChoices: AtomicBoolean = AtomicBoolean(value = false)
    private val isObservingForecastPreferences: AtomicBoolean = AtomicBoolean(value = false)
    private val isExpandOnceEnabled: AtomicBoolean = AtomicBoolean(value = true)
    private val loadJob: AtomicReference<Job?> = AtomicReference(value = null)
    private val searchJob: AtomicReference<Job?> = AtomicReference(value = null)
    private val warningJob: AtomicReference<Job?> = AtomicReference(value = null)

    fun onEvent(event: WeatherForecastEvent) {
        when (event) {
            WeatherForecastEvent.OnShown -> onShown()
            WeatherForecastEvent.OnResumed -> onResumed()
            is WeatherForecastEvent.OnLocaleChanged -> onLocaleChanged(languageTag = event.languageTag)
            WeatherForecastEvent.OnRefresh -> onRefresh()
            WeatherForecastEvent.OnErrorShown -> state.update { state: WeatherForecastState ->
                state.copy(errorMessageResource = null)
            }
            is WeatherForecastEvent.OnForecastViewSelected -> state.update { state: WeatherForecastState ->
                state.withForecastView(forecastView = event.forecastView)
            }
            is WeatherForecastEvent.OnDayToggled -> onDayToggled(dayKey = event.dayKey)
            is WeatherForecastEvent.OnLocationPermissionResult -> onLocationPermissionResult(isGranted = event.isGranted)
            WeatherForecastEvent.OnAutomaticLocationClicked -> state.update { state: WeatherForecastState ->
                state.copy(isLocationPermissionRequested = true)
            }
            is WeatherForecastEvent.OnFavoriteClicked -> onFavoriteClicked(favorite = event.favorite)
            is WeatherForecastEvent.OnFavoriteRemoveClicked -> onFavoriteRemoveClicked(favoriteId = event.favoriteId)
            WeatherForecastEvent.OnAddPlaceClicked -> state.update { state: WeatherForecastState ->
                state.copy(search = SearchState())
            }
            WeatherForecastEvent.OnSearchDismissed -> onSearchDismissed()
            WeatherForecastEvent.OnSearchSubmitted -> onSearchSubmitted()
            is WeatherForecastEvent.OnSearchQueryChanged -> onSearchQueryChanged(query = event.query)
            is WeatherForecastEvent.OnSuggestionClicked -> onSuggestionClicked(place = event.place)
        }
    }

    private fun onDayToggled(dayKey: String) {
        viewModelScope.launch(context = coroutineDispatcher) {
            state.update { state: WeatherForecastState ->
                state.withComingDays(
                    comingDays = state.forecast.comingDays.withDayToggled(
                        dayKey = dayKey,
                        isExpandOnceEnabled = isExpandOnceEnabled.load()
                    )
                )
            }
        }
    }

    private fun onShown() {
        if (isObservingLocationChoices.compareAndSet(expectedValue = false, newValue = true)) {
            observeLocationChoices()
        }
        val isFirstForecastPreferencesObservation: Boolean = isObservingForecastPreferences.compareAndSet(
            expectedValue = false,
            newValue = true
        )
        if (isFirstForecastPreferencesObservation) {
            observeForecastPreferences()
        }
        viewModelScope.launch(context = coroutineDispatcher) {
            getLocationModeTask()
                .onResult(
                    onError = { failure: Failure -> displayError(failure = failure) },
                    onSuccess = { isAutomatic: Boolean ->
                        if (isAutomatic != loadedLocationMode.load()) {
                            startLoading(isAutomatic = isAutomatic)
                        }
                    }
                )
        }
    }

    private fun observeForecastPreferences() {
        viewModelScope.launch(context = coroutineDispatcher) {
            observeForecastPreferencesTask().collect { result: Either<Failure, ForecastPreferences> ->
                result.onResult(
                    onError = { failure: Failure -> displayError(failure = failure) },
                    onSuccess = { forecastPreferences: ForecastPreferences ->
                        isExpandOnceEnabled.store(newValue = forecastPreferences.isExpandOnceEnabled)
                        state.update { state: WeatherForecastState ->
                            state
                                .copy(isHourlyForecastEnabled = forecastPreferences.isHourlyForecastEnabled)
                                .withForecastView(forecastView = state.forecastView)
                        }
                    }
                )
            }
        }
    }

    private fun WeatherForecastState.withForecastView(forecastView: ForecastView): WeatherForecastState =
        copy(
            forecastView = if (isHourlyForecastEnabled) {
                forecastView
            } else {
                ForecastView.DAYS
            }
        )

    private fun observeLocationChoices() {
        viewModelScope.launch(context = coroutineDispatcher) {
            observeLocationChoicesTask().collect { result: Either<Failure, LocationChoices> ->
                result.onResult(
                    onError = { failure: Failure -> displayError(failure = failure) },
                    onSuccess = { locationChoices: LocationChoices ->
                        state.update { state: WeatherForecastState ->
                            state.copy(locationChoices = locationChoices)
                        }
                    }
                )
            }
        }
    }

    private fun onFavoriteClicked(favorite: Favorite) {
        viewModelScope.launch(context = coroutineDispatcher) {
            selectFavoriteTask(favorite = favorite)
                .onResult(
                    onError = { failure: Failure -> displayError(failure = failure) },
                    onSuccess = { reloadStoredLocation() }
                )
        }
    }

    private fun onFavoriteRemoveClicked(favoriteId: String) {
        viewModelScope.launch(context = coroutineDispatcher) {
            val failure: Failure? = removeFavoriteTask(favoriteId = favoriteId).errorOrNull
            if (failure != null) {
                displayError(failure = failure)
            }
        }
    }

    private fun reloadStoredLocation() {
        state.update { state: WeatherForecastState ->
            state.copy(
                search = null,
                isRefreshing = true
            )
        }
        load(
            isAutomatic = false,
            isRefresh = false
        )
    }

    private fun startLoading(isAutomatic: Boolean) {
        if (isAutomatic) {
            state.update { state: WeatherForecastState ->
                state.copy(isLocationPermissionRequested = true)
            }
        } else {
            load(
                isAutomatic = false,
                isRefresh = false
            )
        }
    }

    private fun onLocationPermissionResult(isGranted: Boolean) {
        state.update { state: WeatherForecastState ->
            state.copy(isLocationPermissionRequested = false)
        }
        viewModelScope.launch(context = coroutineDispatcher) {
            setLocationModeTask(isAutomatic = isGranted)
                .onResult(
                    onError = { failure: Failure -> displayError(failure = failure) },
                    onSuccess = {
                        load(
                            isAutomatic = isGranted,
                            isRefresh = false
                        )
                    }
                )
        }
    }

    private fun onResumed() {
        val lastFetchedAt: Instant = fetchedAt.load() ?: return
        if (clock.now() - lastFetchedAt > staleForecastAge) {
            onRefresh()
        }
    }

    private fun onRefresh() {
        val isAutomatic: Boolean = loadedLocationMode.load() ?: return
        state.update { state: WeatherForecastState ->
            state.copy(isRefreshing = true)
        }
        load(
            isAutomatic = isAutomatic,
            isRefresh = true
        )
    }

    private fun load(
        isAutomatic: Boolean,
        isRefresh: Boolean
    ) {
        loadedLocationMode.store(newValue = isAutomatic)
        viewModelScope.launchReplacing(
            currentJob = loadJob,
            context = coroutineDispatcher
        ) {
            getWeatherForecastTask()
                .onResult(
                    onError = { failure: Failure -> displayError(failure = failure) },
                    onSuccess = { placeForecast: PlaceForecast ->
                        displayForecast(placeForecast = placeForecast)
                        observeWarnings(
                            placeForecast = placeForecast,
                            isRefresh = isRefresh
                        )
                    }
                )
        }
    }

    private fun observeWarnings(
        placeForecast: PlaceForecast,
        isRefresh: Boolean
    ) {
        viewModelScope.launchReplacing(
            currentJob = warningJob,
            context = coroutineDispatcher
        ) {
            observeUnseenWarningsTask(
                warningPlace = WarningPlace(
                    latitude = placeForecast.latitude,
                    longitude = placeForecast.longitude,
                    county = placeForecast.county,
                    municipality = placeForecast.municipality
                ),
                isRefresh = isRefresh
            ).collect { result: Either<Failure, List<AggregatedFeed>> ->
                result.onResult(
                    onError = {},
                    onSuccess = { warnings: List<AggregatedFeed> ->
                        unseenWarnings.store(newValue = warnings)
                        displayLatestWarning()
                    }
                )
            }
        }
    }

    private fun displayLatestWarning() {
        state.update { state: WeatherForecastState ->
            state.copy(
                latestWarning = unseenWarnings.load().toLatestWarning(
                    now = clock.now(),
                    timeZone = TimeZone.currentSystemDefault(),
                    languageTag = languageTag.load()
                )
            )
        }
    }

    private fun displayForecast(placeForecast: PlaceForecast) {
        fetchedAt.store(newValue = clock.now())
        val previousPlaceForecast: PlaceForecast? = this.placeForecast.exchange(newValue = placeForecast)
        val isSamePlace: Boolean = previousPlaceForecast?.placeName == placeForecast.placeName
        state.update { state: WeatherForecastState ->
            state
                .withForecast(
                    forecast = placeForecast.toForecast(),
                    isSamePlace = isSamePlace
                ).copy(
                    localityName = placeForecast.placeName,
                    isLoading = false,
                    isRefreshing = false
                )
        }
    }

    private fun onLocaleChanged(languageTag: String) {
        if (this.languageTag.exchange(newValue = languageTag) == languageTag) {
            return
        }
        viewModelScope.launch(context = coroutineDispatcher) {
            displayLatestWarning()
            val loadedPlaceForecast: PlaceForecast = placeForecast.load() ?: return@launch
            state.update { state: WeatherForecastState ->
                state.withForecast(
                    forecast = loadedPlaceForecast.toForecast(),
                    isSamePlace = true
                )
            }
        }
    }

    private fun PlaceForecast.toForecast(): WeatherForecast =
        toWeatherForecast(
            now = clock.now(),
            timeZone = TimeZone.currentSystemDefault(),
            languageTag = languageTag.load()
        )

    private fun onSearchDismissed() {
        searchJob.exchange(newValue = null)?.cancel()
        state.update { state: WeatherForecastState ->
            state.copy(search = null)
        }
    }

    private fun onSearchQueryChanged(query: String) {
        state.update { state: WeatherForecastState ->
            val search: SearchState? = state.search
            if (search != null) {
                state.copy(search = search.copy(query = query))
            } else {
                state
            }
        }
        viewModelScope.launchReplacing(
            currentJob = searchJob,
            context = coroutineDispatcher
        ) {
            delay(duration = searchDebounce)
            search(query = query).onResult(
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

    private suspend fun search(query: String): Either<Failure, List<Place>> =
        if (query.isBlank()) {
            emptyList<Place>().asRight()
        } else {
            searchPlacesTask(prefix = query)
        }

    private fun displaySuggestions(
        query: String,
        suggestions: ImmutableList<Place>
    ) {
        state.update { state: WeatherForecastState ->
            val search: SearchState? = state.search
            if (search != null && search.query == query) {
                state.copy(search = search.copy(suggestions = suggestions))
            } else {
                state
            }
        }
    }

    private fun onSearchSubmitted() {
        val place: Place = state.value.search?.suggestions?.firstOrNull() ?: return
        onSuggestionClicked(place = place)
    }

    private fun onSuggestionClicked(place: Place) {
        searchJob.exchange(newValue = null)?.cancel()
        viewModelScope.launch(context = coroutineDispatcher) {
            selectPlaceTask(place = place)
                .onResult(
                    onError = { failure: Failure -> displayError(failure = failure) },
                    onSuccess = { reloadStoredLocation() }
                )
        }
    }

    private fun displayError(failure: Failure) {
        state.update { state: WeatherForecastState ->
            state.copy(
                isLoading = false,
                isRefreshing = false,
                errorMessageResource = failure.messageResource()
            )
        }
    }
}