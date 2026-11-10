package se.vadretapp.vadret.weatherforecast

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.errorOrNull
import io.github.sphrak.either.extension.onResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.AddFavoriteTask
import se.vadretapp.vadret.followedplace.GetChosenPlaceTask
import se.vadretapp.vadret.followedplace.ObserveLocationChoicesTask
import se.vadretapp.vadret.followedplace.RemoveFavoriteTask
import se.vadretapp.vadret.followedplace.SelectFavoriteTask
import se.vadretapp.vadret.followedplace.SetLocationModeTask
import se.vadretapp.vadret.followedplace.model.FavoriteChoice
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.followedplace.model.LocationChoices
import se.vadretapp.vadret.forecastpreference.model.ForecastPreferences
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.setting.model.HourFormat
import se.vadretapp.vadret.ui.extension.launchReplacing
import se.vadretapp.vadret.ui.extension.messageResource
import se.vadretapp.vadret.ui.format.TextLocale
import se.vadretapp.vadret.ui.format.currentLanguageTag
import se.vadretapp.vadret.ui.format.toDayTimeText
import se.vadretapp.vadret.warning.model.WarningPlace
import se.vadretapp.vadret.warning.usecase.ObserveUnseenFollowedPlaceWarningsTask
import se.vadretapp.vadret.warning.usecase.ObserveUnseenWarningsTask
import se.vadretapp.vadret.weatherforecast.extension.isAt
import se.vadretapp.vadret.weatherforecast.extension.localityFor
import se.vadretapp.vadret.weatherforecast.extension.toLatestWarning
import se.vadretapp.vadret.weatherforecast.extension.toWeatherForecast
import se.vadretapp.vadret.weatherforecast.extension.withComingDays
import se.vadretapp.vadret.weatherforecast.extension.withDayExpanded
import se.vadretapp.vadret.weatherforecast.extension.withDayToggled
import se.vadretapp.vadret.weatherforecast.extension.withForecast
import se.vadretapp.vadret.weatherforecast.extension.withNameTaken
import se.vadretapp.vadret.weatherforecast.extension.withOtherDaysClosed
import se.vadretapp.vadret.weatherforecast.extension.withoutName
import se.vadretapp.vadret.weatherforecast.model.DayToShow
import se.vadretapp.vadret.weatherforecast.model.ForecastView
import se.vadretapp.vadret.weatherforecast.model.PlaceForecast
import se.vadretapp.vadret.weatherforecast.model.PlaceNaming
import se.vadretapp.vadret.weatherforecast.model.WeatherForecast
import se.vadretapp.vadret.weatherforecast.usecase.GetLocationModeTask
import se.vadretapp.vadret.weatherforecast.usecase.ObserveForecastPreferencesTask
import se.vadretapp.vadret.weatherforecast.usecase.ObservePlaceForecastTask
import se.vadretapp.vadret.weatherforecast.usecase.ObserveWeatherForecastTask
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val staleForecastAge: Duration = 2.minutes

@Immutable
class WeatherForecastViewModel internal constructor(
    private val getLocationModeTask: GetLocationModeTask,
    private val getChosenPlaceTask: GetChosenPlaceTask,
    private val setLocationModeTask: SetLocationModeTask,
    private val observeWeatherForecastTask: ObserveWeatherForecastTask,
    private val observeLocationChoicesTask: ObserveLocationChoicesTask,
    private val observeForecastPreferencesTask: ObserveForecastPreferencesTask,
    private val selectFavoriteTask: SelectFavoriteTask,
    private val removeFavoriteTask: RemoveFavoriteTask,
    private val observePlaceForecastTask: ObservePlaceForecastTask,
    private val addFavoriteTask: AddFavoriteTask,
    private val observeUnseenWarningsTask: ObserveUnseenWarningsTask,
    private val observeUnseenFollowedPlaceWarningsTask: ObserveUnseenFollowedPlaceWarningsTask,
    private val clock: Clock,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModel() {

    val state: StateFlow<WeatherForecastState>
        field = MutableStateFlow(value = WeatherForecastState())

    private val loadedLocationMode: AtomicReference<Boolean?> = AtomicReference(value = null)

    private val droppedPlace: AtomicReference<Location?> = AtomicReference(value = null)
    private val textLocale: AtomicReference<TextLocale> = AtomicReference(
        value = TextLocale(
            languageTag = currentLanguageTag(),
            hourFormat = HourFormat.TWENTY_FOUR_HOUR
        )
    )
    private val textLocaleJob: AtomicReference<Job?> = AtomicReference(value = null)
    private val placeForecast: AtomicReference<PlaceForecast?> = AtomicReference(value = null)
    private val unseenWarnings: AtomicReference<List<AggregatedFeed>> = AtomicReference(value = emptyList())
    private val fetchedAt: AtomicReference<Instant?> = AtomicReference(value = null)
    private val isObservingLocationChoices: AtomicBoolean = AtomicBoolean(value = false)
    private val isObservingForecastPreferences: AtomicBoolean = AtomicBoolean(value = false)
    private val isExpandOnceEnabled: AtomicBoolean = AtomicBoolean(value = true)
    private val loadJob: AtomicReference<Job?> = AtomicReference(value = null)
    private val warningJob: AtomicReference<Job?> = AtomicReference(value = null)

    fun onEvent(event: WeatherForecastEvent) {
        when (event) {
            WeatherForecastEvent.OnShown -> onShown()
            is WeatherForecastEvent.OnPlaceShown -> onPlaceShown(
                location = Location(
                    latitude = event.latitude,
                    longitude = event.longitude
                )
            )
            WeatherForecastEvent.OnSavePlaceClicked -> onSavePlaceClicked()
            is WeatherForecastEvent.OnPlaceLabelChanged -> state.update { state: WeatherForecastState ->
                state.copy(
                    placeNaming = state.placeNaming?.copy(
                        label = event.label,
                        isLabelMissing = false
                    )
                )
            }
            WeatherForecastEvent.OnPlaceLabelDismissed -> state.update { state: WeatherForecastState ->
                state.copy(placeNaming = null)
            }
            WeatherForecastEvent.OnResumed -> onResumed()
            is WeatherForecastEvent.OnLocaleChanged -> onLocaleChanged(
                languageTag = event.languageTag,
                hourFormat = event.hourFormat
            )
            WeatherForecastEvent.OnRefresh -> onRefresh()
            WeatherForecastEvent.OnErrorShown -> state.update { state: WeatherForecastState ->
                state.copy(errorMessageResource = null)
            }
            is WeatherForecastEvent.OnForecastViewSelected -> state.update { state: WeatherForecastState ->
                state.withForecastView(forecastView = event.forecastView)
            }
            is WeatherForecastEvent.OnDayToggled -> onDayToggled(dayKey = event.dayKey)
            is WeatherForecastEvent.OnWeekDayClicked -> onWeekDayClicked(dayKey = event.dayKey)
            is WeatherForecastEvent.OnDayScrolledTo -> onDayScrolledTo(dayKey = event.dayKey)
            is WeatherForecastEvent.OnDayShown -> onDayShown(dayKey = event.dayKey)
            is WeatherForecastEvent.OnLocationPermissionResult -> onLocationPermissionResult(isGranted = event.isGranted)
            WeatherForecastEvent.OnAutomaticLocationClicked -> state.update { state: WeatherForecastState ->
                state.copy(isLocationPermissionRequested = true)
            }
            is WeatherForecastEvent.OnFavoriteClicked -> onFavoriteClicked(favorite = event.favorite)
            is WeatherForecastEvent.OnFavoriteRemoveClicked -> onFavoriteRemoveClicked(favoriteId = event.favoriteId)
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

    private fun onWeekDayClicked(dayKey: String) {
        viewModelScope.launch(context = coroutineDispatcher) {
            state.update { state: WeatherForecastState ->
                state.withComingDays(
                    comingDays = state.forecast.comingDays.withOtherDaysClosed(
                        dayKey = dayKey,
                        isExpandOnceEnabled = isExpandOnceEnabled.load()
                    )
                ).copy(
                    forecastView = ForecastView.DAYS,
                    dayToShow = DayToShow(
                        dayKey = dayKey,
                        isScrolledTo = false
                    )
                )
            }
        }
    }

    private fun onDayScrolledTo(dayKey: String) {
        viewModelScope.launch(context = coroutineDispatcher) {
            state.update { state: WeatherForecastState ->
                if (state.dayToShow?.dayKey == dayKey) {
                    state.withComingDays(
                        comingDays = state.forecast.comingDays.withDayExpanded(
                            dayKey = dayKey,
                            isExpandOnceEnabled = isExpandOnceEnabled.load()
                        )
                    ).copy(
                        dayToShow = DayToShow(
                            dayKey = dayKey,
                            isScrolledTo = true
                        )
                    )
                } else {
                    state
                }
            }
        }
    }

    private fun onDayShown(dayKey: String) {
        state.update { state: WeatherForecastState ->
            if (state.dayToShow?.dayKey == dayKey) {
                state.copy(dayToShow = null)
            } else {
                state
            }
        }
    }

    private fun onShown() {
        startObserving()
        viewModelScope.launch(context = coroutineDispatcher) {
            getLocationModeTask()
                .onResult(
                    onError = { failure: Failure -> displayError(failure = failure) },
                    onSuccess = { isAutomatic: Boolean ->
                        if (isAutomatic != loadedLocationMode.load()) {
                            startLoading(isAutomatic = isAutomatic)
                        } else if (!isAutomatic) {
                            reloadIfAnotherPlaceWasChosen()
                        }
                    }
                )
        }
    }

    // A place opened from the map can become the chosen location while this screen is out of sight.
    private suspend fun reloadIfAnotherPlaceWasChosen() {
        val loadedPlaceForecast: PlaceForecast = placeForecast.load() ?: return
        getChosenPlaceTask().onResult(
            onError = { failure: Failure -> displayError(failure = failure) },
            onSuccess = { chosenPlace: FollowedPlace ->
                if (!loadedPlaceForecast.isAt(followedPlace = chosenPlace)) {
                    reloadStoredLocation()
                }
            }
        )
    }

    private fun onPlaceShown(location: Location) {
        startObserving()
        if (droppedPlace.exchange(newValue = location) != location) {
            loadForecast(isRefresh = false)
        }
    }

    private fun startObserving() {
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
                            state.copy(locationChoices = locationChoices).withPlaceSaved()
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
            state.copy(isRefreshing = true)
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
        if (droppedPlace.load() == null && loadedLocationMode.load() == null) {
            return
        }
        state.update { state: WeatherForecastState ->
            state.copy(isRefreshing = true)
        }
        loadForecast(isRefresh = true)
    }

    private fun load(
        isAutomatic: Boolean,
        isRefresh: Boolean
    ) {
        loadedLocationMode.store(newValue = isAutomatic)
        loadForecast(isRefresh = isRefresh)
    }

    private fun loadForecast(isRefresh: Boolean) {
        viewModelScope.launchReplacing(
            currentJob = loadJob,
            context = coroutineDispatcher
        ) {
            val isObservingWarnings = AtomicBoolean(value = false)
            observeForecast(isRefresh = isRefresh).collect { result: Either<Failure, PlaceForecast> ->
                result.onResult(
                    onError = { failure: Failure -> displayError(failure = failure) },
                    onSuccess = { placeForecast: PlaceForecast ->
                        displayForecast(placeForecast = placeForecast)
                        if (isObservingWarnings.compareAndSet(expectedValue = false, newValue = true)) {
                            observeWarnings(
                                placeForecast = placeForecast,
                                isRefresh = isRefresh
                            )
                        }
                    }
                )
            }
        }
    }

    private fun observeForecast(isRefresh: Boolean): Flow<Either<Failure, PlaceForecast>> {
        val place: Location = droppedPlace.load() ?: return observeWeatherForecastTask(isRefresh = isRefresh)
        return observePlaceForecastTask(
            latitude = place.latitude,
            longitude = place.longitude,
            isRefresh = isRefresh
        )
    }

    private fun observeWarnings(
        placeForecast: PlaceForecast,
        isRefresh: Boolean
    ) {
        viewModelScope.launchReplacing(
            currentJob = warningJob,
            context = coroutineDispatcher
        ) {
            unseenWarningsFor(
                placeForecast = placeForecast,
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

    // A dropped place shows its own warnings, the chosen location those of every followed place.
    private fun unseenWarningsFor(
        placeForecast: PlaceForecast,
        isRefresh: Boolean
    ): Flow<Either<Failure, List<AggregatedFeed>>> =
        if (droppedPlace.load() != null) {
            observeUnseenWarningsTask(
                warningPlace = WarningPlace(
                    latitude = placeForecast.latitude,
                    longitude = placeForecast.longitude,
                    county = placeForecast.county,
                    municipality = placeForecast.municipality
                ),
                isRefresh = isRefresh
            )
        } else {
            observeUnseenFollowedPlaceWarningsTask(isRefresh = isRefresh)
        }

    private fun displayLatestWarning() {
        val shownTextLocale: TextLocale = textLocale.load()
        state.update { state: WeatherForecastState ->
            state.copy(
                latestWarning = unseenWarnings.load().toLatestWarning(
                    now = clock.now(),
                    hourFormat = shownTextLocale.hourFormat,
                    timeZone = TimeZone.currentSystemDefault(),
                    languageTag = shownTextLocale.languageTag
                )
            )
        }
    }

    private fun displayForecast(placeForecast: PlaceForecast) {
        fetchedAt.store(newValue = placeForecast.fetchedAt)
        val previousPlaceForecast: PlaceForecast? = this.placeForecast.exchange(newValue = placeForecast)
        val isSamePlace: Boolean = previousPlaceForecast?.placeName == placeForecast.placeName
        state.update { state: WeatherForecastState ->
            state
                .withForecast(
                    forecast = placeForecast.toForecast(),
                    isSamePlace = isSamePlace
                ).copy(
                    localityName = placeForecast.placeName,
                    outdatedForecastTimeText = placeForecast.outdatedTimeText(),
                    isLoading = false,
                    isRefreshing = false
                ).withPlaceSaved()
        }
    }

    // A place saved under a name of its own shows that name, not the one Nominatim found.
    private fun WeatherForecastState.withPlaceSaved(): WeatherForecastState {
        val loadedPlaceForecast: PlaceForecast = placeForecast.load() ?: return this
        val savedFavorite: Favorite? = locationChoices.favorites
            .firstOrNull { favoriteChoice: FavoriteChoice -> favoriteChoice.favorite.isAt(placeForecast = loadedPlaceForecast) }
            ?.favorite
        return copy(
            localityName = savedFavorite?.locality ?: loadedPlaceForecast.placeName,
            isPlaceSaved = savedFavorite != null
        )
    }

    private fun onSavePlaceClicked() {
        val droppedPlaceForecast: PlaceForecast = placeForecast.load() ?: return
        val placeNaming: PlaceNaming? = state.value.placeNaming
        val locality: String? = placeNaming.localityFor(placeName = droppedPlaceForecast.placeName)
        if (locality == null) {
            state.update { state: WeatherForecastState ->
                state.copy(placeNaming = placeNaming.withoutName())
            }
            return
        }
        viewModelScope.launch(context = coroutineDispatcher) {
            addFavoriteTask(
                locality = locality,
                municipality = droppedPlaceForecast.municipality.orEmpty(),
                county = droppedPlaceForecast.county.orEmpty(),
                latitude = droppedPlaceForecast.latitude,
                longitude = droppedPlaceForecast.longitude
            ).onResult(
                onError = { failure: Failure ->
                    if (failure.errorCode == ErrorCode.FAVORITE_NAME_TAKEN) {
                        state.update { state: WeatherForecastState ->
                            state.copy(placeNaming = state.placeNaming.withNameTaken(placeName = locality))
                        }
                    } else {
                        displayError(failure = failure)
                    }
                },
                onSuccess = { displaySavedPlace(locality = locality) }
            )
        }
    }

    private fun displaySavedPlace(locality: String) {
        state.update { state: WeatherForecastState ->
            state.copy(
                localityName = locality,
                isPlaceSaved = true,
                placeNaming = null
            )
        }
    }

    private fun onLocaleChanged(
        languageTag: String,
        hourFormat: HourFormat
    ) {
        val shownTextLocale = TextLocale(
            languageTag = languageTag,
            hourFormat = hourFormat
        )
        if (textLocale.exchange(newValue = shownTextLocale) == shownTextLocale) {
            return
        }
        // The language and clock change together and the newest change is the one shown, however the redrawing interleaves.
        viewModelScope.launchReplacing(
            currentJob = textLocaleJob,
            context = coroutineDispatcher
        ) {
            displayLatestWarning()
            val loadedPlaceForecast: PlaceForecast = placeForecast.load() ?: return@launchReplacing
            state.update { state: WeatherForecastState ->
                state
                    .withForecast(
                        forecast = loadedPlaceForecast.toForecast(),
                        isSamePlace = true
                    ).copy(outdatedForecastTimeText = loadedPlaceForecast.outdatedTimeText())
            }
        }
    }

    private fun PlaceForecast.toForecast(): WeatherForecast {
        val shownTextLocale: TextLocale = textLocale.load()
        return toWeatherForecast(
            now = clock.now(),
            timeZone = TimeZone.currentSystemDefault(),
            languageTag = shownTextLocale.languageTag,
            hourFormat = shownTextLocale.hourFormat
        )
    }

    private fun PlaceForecast.outdatedTimeText(): String? {
        val shownTextLocale: TextLocale = textLocale.load()
        return if (isRefreshFailed) {
            fetchedAt?.toDayTimeText(
                now = clock.now(),
                hourFormat = shownTextLocale.hourFormat,
                timeZone = TimeZone.currentSystemDefault(),
                languageTag = shownTextLocale.languageTag
            )
        } else {
            null
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