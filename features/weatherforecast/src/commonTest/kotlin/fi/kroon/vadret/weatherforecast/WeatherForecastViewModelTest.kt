package fi.kroon.vadret.weatherforecast

import fi.kroon.vadret.aggregatedfeed.model.AggregatedFeed
import fi.kroon.vadret.aggregatedfeed.model.FeedSource
import fi.kroon.vadret.aggregatedfeed.model.WarningLevel
import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.core.resources.Res
import fi.kroon.vadret.core.resources.data_not_available_at_this_location
import fi.kroon.vadret.core.resources.unhandled_error
import fi.kroon.vadret.testing.FixedClock
import fi.kroon.vadret.ui.format.currentLanguageTag
import fi.kroon.vadret.ui.format.toDayTimeText
import fi.kroon.vadret.warning.model.WarningPlace
import fi.kroon.vadret.weatherforecast.model.ComingDay
import fi.kroon.vadret.weatherforecast.model.FavoriteChoice
import fi.kroon.vadret.weatherforecast.model.Forecast
import fi.kroon.vadret.weatherforecast.model.ForecastDay
import fi.kroon.vadret.weatherforecast.model.ForecastView
import fi.kroon.vadret.weatherforecast.model.LatestWarning
import fi.kroon.vadret.weatherforecast.model.LocationChoices
import fi.kroon.vadret.weatherforecast.model.SearchState
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val threeDays: List<Forecast> = listOf(
    noon,
    noon.copy(
        validAt = Instant.parse(input = "2026-10-25T12:00:00Z"),
        intervalStartedAt = Instant.parse(input = "2026-10-25T11:00:00Z")
    ),
    noon.copy(
        validAt = Instant.parse(input = "2026-10-26T12:00:00Z"),
        intervalStartedAt = Instant.parse(input = "2026-10-26T11:00:00Z")
    )
)

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherForecastViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val locationRepository = FakeLocationRepository()
    private val weatherForecastRepository = FakeWeatherForecastRepository()
    private val placeRepository = FakePlaceRepository()
    private val favoriteRepository = FakeFavoriteRepository()
    private val clock = FixedClock(now = Instant.parse(input = "2026-10-24T12:00:00Z"))
    private val observeUnseenWarningsTask = FakeObserveUnseenWarningsTask()
    private val forecastPreferenceRepository = FakeForecastPreferenceRepository()
    private val weatherForecastViewModel = WeatherForecastViewModel(
        getLocationModeTask = DefaultGetLocationModeTask(locationRepository = locationRepository),
        setLocationModeTask = DefaultSetLocationModeTask(locationRepository = locationRepository),
        getWeatherForecastTask = DefaultGetWeatherForecastTask(
            locationRepository = locationRepository,
            weatherForecastRepository = weatherForecastRepository,
            nominatimRepository = FakeNominatimRepository()
        ),
        observeLocationChoicesTask = DefaultObserveLocationChoicesTask(
            favoriteRepository = favoriteRepository,
            locationRepository = locationRepository
        ),
        observeForecastPreferencesTask = DefaultObserveForecastPreferencesTask(forecastPreferenceRepository = forecastPreferenceRepository),
        selectFavoriteTask = DefaultSelectFavoriteTask(locationRepository = locationRepository),
        removeFavoriteTask = DefaultRemoveFavoriteTask(favoriteRepository = favoriteRepository),
        searchPlacesTask = DefaultSearchPlacesTask(placeRepository = placeRepository),
        selectPlaceTask = DefaultSelectPlaceTask(
            favoriteRepository = favoriteRepository,
            locationRepository = locationRepository
        ),
        observeUnseenWarningsTask = observeUnseenWarningsTask,
        clock = clock,
        coroutineDispatcher = testDispatcher
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a stored place is loaded when shown`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            val state: WeatherForecastState = weatherForecastViewModel.state.value
            assertEquals(expected = "Stockholm", actual = state.localityName)
            assertEquals(expected = false, actual = state.isLoading)
            assertTrue(actual = state.forecast.days.isNotEmpty())
        }

    @Test
    fun `showing again only reloads when the mode changed`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()
            assertEquals(expected = 1, actual = weatherForecastRepository.requestedCoordinates.size)

            locationRepository.isAutomatic = true.asRight()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            assertTrue(actual = weatherForecastViewModel.state.value.isLocationPermissionRequested)
            assertEquals(expected = 1, actual = weatherForecastRepository.requestedCoordinates.size)
        }

    @Test
    fun `the permission answer sets the mode and loads`() =
        testScope.runTest {
            locationRepository.isAutomatic = true.asRight()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnLocationPermissionResult(isGranted = false))
            advanceUntilIdle()

            assertEquals(expected = false, actual = weatherForecastViewModel.state.value.isLocationPermissionRequested)
            assertEquals(expected = listOf("automatic=false"), actual = locationRepository.writes)
            assertEquals(expected = listOf(59.3293 to 18.0686), actual = weatherForecastRepository.requestedCoordinates)
        }

    @Test
    fun `a refresh before the first load does nothing`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnRefresh)
            advanceUntilIdle()

            assertEquals(expected = false, actual = weatherForecastViewModel.state.value.isRefreshing)
            assertTrue(actual = weatherForecastRepository.requestedCoordinates.isEmpty())
        }

    @Test
    fun `resuming more than two minutes after the last fetch refreshes the forecast`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            clock.now += 2.minutes + 1.seconds
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnResumed)
            val whileRefreshing: Boolean = weatherForecastViewModel.state.value.isRefreshing
            advanceUntilIdle()

            assertTrue(actual = whileRefreshing)
            assertEquals(expected = false, actual = weatherForecastViewModel.state.value.isRefreshing)
            assertEquals(expected = 2, actual = weatherForecastRepository.requestedCoordinates.size)
        }

    @Test
    fun `resuming within two minutes of the last fetch keeps the forecast`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            clock.now += 2.minutes
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnResumed)
            advanceUntilIdle()

            assertEquals(expected = false, actual = weatherForecastViewModel.state.value.isRefreshing)
            assertEquals(expected = 1, actual = weatherForecastRepository.requestedCoordinates.size)
        }

    @Test
    fun `resuming before anything was fetched fetches nothing`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnResumed)
            advanceUntilIdle()

            assertTrue(actual = weatherForecastRepository.requestedCoordinates.isEmpty())
        }

    @Test
    fun `the time since the last successful fetch decides a refresh on resume`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()
            weatherForecastRepository.forecasts = Failure(errorCode = ErrorCode.WEATHER_FORECAST_NOT_AVAILABLE_FOR_LOCATION).asLeft()
            clock.now += 1.minutes
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnRefresh)
            advanceUntilIdle()

            clock.now += 1.minutes + 1.seconds
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnResumed)
            advanceUntilIdle()

            assertEquals(expected = 3, actual = weatherForecastRepository.requestedCoordinates.size)
        }

    @Test
    fun `the latest unseen warning for the place is shown once the forecast is shown`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            assertEquals(
                expected = LatestWarning(
                    warningLevel = WarningLevel.YELLOW,
                    source = FeedSource.SMHI,
                    isPublicAlert = false,
                    headline = "Vind",
                    endsAtText = null,
                    warningIdentifiers = persistentListOf("smhi-1", "smhi-2")
                ),
                actual = weatherForecastViewModel.state.value.latestWarning
            )
            assertEquals(
                expected = listOf(
                    WarningPlace(
                        latitude = 59.3293,
                        longitude = 18.0686,
                        county = "Stockholm",
                        municipality = "Stockholm"
                    ) to false
                ),
                actual = observeUnseenWarningsTask.requests
            )
        }

    @Test
    fun `the device location is matched with the county and municipality it lies in`() =
        testScope.runTest {
            locationRepository.isAutomatic = true.asRight()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnLocationPermissionResult(isGranted = true))
            advanceUntilIdle()

            assertEquals(
                expected = listOf(
                    WarningPlace(
                        latitude = 57.7,
                        longitude = 11.97,
                        county = "Västra Götalands län",
                        municipality = "Göteborgs kommun"
                    ) to false
                ),
                actual = observeUnseenWarningsTask.requests
            )
        }

    @Test
    fun `the end of the latest warning is told as a time`() =
        testScope.runTest {
            val endsAt: Instant = clock.now + 30.minutes
            observeUnseenWarningsTask.unseenWarnings.value = listOf(
                warning(
                    identifier = "smhi-1",
                    endsAt = endsAt
                )
            ).asRight()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            assertEquals(
                expected = endsAt.toDayTimeText(
                    now = clock.now,
                    timeZone = TimeZone.currentSystemDefault(),
                    languageTag = currentLanguageTag()
                ),
                actual = weatherForecastViewModel.state.value.latestWarning?.endsAtText
            )
        }

    @Test
    fun `a refresh asks for fresh warnings`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnRefresh)
            advanceUntilIdle()

            assertEquals(
                expected = listOf(false, true),
                actual = observeUnseenWarningsTask.requests.map { request: Pair<WarningPlace, Boolean> -> request.second }
            )
        }

    @Test
    fun `seeing every warning removes the pill`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            observeUnseenWarningsTask.unseenWarnings.value = listOf(warning(identifier = "smhi-2")).asRight()
            advanceUntilIdle()
            assertEquals(
                expected = persistentListOf("smhi-2"),
                actual = weatherForecastViewModel.state.value.latestWarning?.warningIdentifiers
            )

            observeUnseenWarningsTask.unseenWarnings.value = emptyList<AggregatedFeed>().asRight()
            advanceUntilIdle()
            assertNull(actual = weatherForecastViewModel.state.value.latestWarning)
        }

    @Test
    fun `warnings that cannot be loaded keep the last pill without a message`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            observeUnseenWarningsTask.unseenWarnings.value = Failure(errorCode = ErrorCode.NETWORK_FAILURE).asLeft()
            advanceUntilIdle()

            assertEquals(
                expected = persistentListOf("smhi-1", "smhi-2"),
                actual = weatherForecastViewModel.state.value.latestWarning?.warningIdentifiers
            )
            assertNull(actual = weatherForecastViewModel.state.value.errorMessageResource)
        }

    @Test
    fun `no forecast means no warnings are asked for`() =
        testScope.runTest {
            weatherForecastRepository.forecasts = Failure(errorCode = ErrorCode.WEATHER_FORECAST_NOT_AVAILABLE_FOR_LOCATION).asLeft()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            assertEquals(expected = emptyList(), actual = observeUnseenWarningsTask.requests)
            assertNull(actual = weatherForecastViewModel.state.value.latestWarning)
        }

    @Test
    fun `a failed load shows its message until seen`() =
        testScope.runTest {
            weatherForecastRepository.forecasts = Failure(errorCode = ErrorCode.WEATHER_FORECAST_NOT_AVAILABLE_FOR_LOCATION).asLeft()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            assertEquals(
                expected = Res.string.data_not_available_at_this_location,
                actual = weatherForecastViewModel.state.value.errorMessageResource
            )
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnErrorShown)
            assertNull(actual = weatherForecastViewModel.state.value.errorMessageResource)
        }

    @Test
    fun `typing searches once it pauses and keeps only the latest query`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnAddPlaceClicked)
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSearchQueryChanged(query = "K"))
            advanceTimeBy(delayTimeMillis = 30)
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSearchQueryChanged(query = "Ki"))
            advanceUntilIdle()

            assertEquals(expected = listOf("Ki" to 10), actual = placeRepository.searches)
            assertEquals(
                expected = SearchState(
                    query = "Ki",
                    suggestions = persistentListOf(kiruna)
                ),
                actual = weatherForecastViewModel.state.value.search
            )
        }

    @Test
    fun `a blank query clears the suggestions without searching`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnAddPlaceClicked)
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSearchQueryChanged(query = " "))
            advanceUntilIdle()

            assertTrue(actual = placeRepository.searches.isEmpty())
            assertEquals(
                expected = SearchState(
                    query = " ",
                    suggestions = persistentListOf()
                ),
                actual = weatherForecastViewModel.state.value.search
            )
        }

    @Test
    fun `submitting picks the first suggestion and loads its forecast`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnAddPlaceClicked)
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSearchQueryChanged(query = "Kir"))
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSearchSubmitted)
            advanceUntilIdle()

            val state: WeatherForecastState = weatherForecastViewModel.state.value
            assertNull(actual = state.search)
            assertEquals(expected = "Kiruna", actual = state.localityName)
            assertEquals(expected = listOf(67.8558 to 20.2253), actual = weatherForecastRepository.requestedCoordinates)
        }

    @Test
    fun `dismissing the search closes it`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnAddPlaceClicked)
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSearchDismissed)

            assertNull(actual = weatherForecastViewModel.state.value.search)
        }

    @Test
    fun `a picked suggestion becomes the selected favorite`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSuggestionClicked(place = kiruna))
            advanceUntilIdle()

            assertEquals(
                expected = LocationChoices(
                    isAutomatic = false,
                    favorites = persistentListOf(FavoriteChoice(favorite = kirunaFavorite, isSelected = true))
                ),
                actual = weatherForecastViewModel.state.value.locationChoices
            )
        }

    @Test
    fun `a clicked favorite loads its forecast`() =
        testScope.runTest {
            favoriteRepository.favorites.value = listOf(kirunaFavorite)
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnFavoriteClicked(favorite = kirunaFavorite))
            advanceUntilIdle()

            val state: WeatherForecastState = weatherForecastViewModel.state.value
            assertEquals(expected = "Kiruna", actual = state.localityName)
            assertEquals(expected = false, actual = state.isRefreshing)
            assertEquals(expected = 67.8558 to 20.2253, actual = weatherForecastRepository.requestedCoordinates.last())
            assertEquals(
                expected = listOf(true),
                actual = state.locationChoices.favorites.map { favoriteChoice: FavoriteChoice -> favoriteChoice.isSelected }
            )
        }

    @Test
    fun `a removed favorite leaves the choices`() =
        testScope.runTest {
            favoriteRepository.favorites.value = listOf(kirunaFavorite)
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnFavoriteRemoveClicked(favoriteId = kirunaFavorite.favoriteId))
            advanceUntilIdle()

            assertEquals(
                expected = persistentListOf(),
                actual = weatherForecastViewModel.state.value.locationChoices.favorites
            )
        }

    @Test
    fun `a favorite that cannot be removed shows a message`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()
            favoriteRepository.failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnFavoriteRemoveClicked(favoriteId = kirunaFavorite.favoriteId))
            advanceUntilIdle()

            assertEquals(
                expected = Res.string.unhandled_error,
                actual = weatherForecastViewModel.state.value.errorMessageResource
            )
        }

    @Test
    fun `choosing automatic asks for the location permission`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnAutomaticLocationClicked)

            assertTrue(actual = weatherForecastViewModel.state.value.isLocationPermissionRequested)
        }

    @Test
    fun `location choices are observed once however often the screen is shown`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            assertEquals(expected = 1, actual = favoriteRepository.favorites.subscriptionCount.value)
        }

    @Test
    fun `nothing is observed before the screen is shown`() =
        testScope.runTest {
            advanceUntilIdle()

            assertEquals(expected = 0, actual = favoriteRepository.favorites.subscriptionCount.value)
        }

    @Test
    fun `a new language rewrites the day headings of the loaded forecast`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnLocaleChanged(languageTag = "en"))
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()
            val english: ForecastDay = weatherForecastViewModel.state.value.forecast.days.first()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnLocaleChanged(languageTag = "sv-SE"))
            advanceUntilIdle()
            val swedish: ForecastDay = weatherForecastViewModel.state.value.forecast.days.first()

            assertEquals(expected = "Saturday" to "October 24, 2026", actual = english.weekday to english.dateText)
            assertEquals(expected = "Lördag" to "24 oktober 2026", actual = swedish.weekday to swedish.dateText)
            assertEquals(expected = "Stockholm", actual = weatherForecastViewModel.state.value.localityName)
        }

    @Test
    fun `a new language before any forecast changes nothing`() =
        testScope.runTest {
            val before: WeatherForecastState = weatherForecastViewModel.state.value

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnLocaleChanged(languageTag = "sv-SE"))
            advanceUntilIdle()

            assertEquals(expected = before, actual = weatherForecastViewModel.state.value)
        }

    @Test
    fun `days is the default view and hours can be picked once the hourly forecast is on`() =
        testScope.runTest {
            assertEquals(expected = false, actual = weatherForecastViewModel.state.value.isHourlyForecastEnabled)
            assertEquals(expected = ForecastView.DAYS, actual = weatherForecastViewModel.state.value.forecastView)
            forecastPreferenceRepository.setHourlyForecastEnabled(isEnabled = true)
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnForecastViewSelected(forecastView = ForecastView.HOURS))
            advanceUntilIdle()
            assertEquals(expected = ForecastView.HOURS, actual = weatherForecastViewModel.state.value.forecastView)

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnForecastViewSelected(forecastView = ForecastView.DAYS))
            advanceUntilIdle()
            assertEquals(expected = ForecastView.DAYS, actual = weatherForecastViewModel.state.value.forecastView)
        }

    @Test
    fun `turning the hourly forecast off hides the switcher and shows the days`() =
        testScope.runTest {
            forecastPreferenceRepository.setHourlyForecastEnabled(isEnabled = true)
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnForecastViewSelected(forecastView = ForecastView.HOURS))
            advanceUntilIdle()

            forecastPreferenceRepository.setHourlyForecastEnabled(isEnabled = false)
            advanceUntilIdle()

            val state: WeatherForecastState = weatherForecastViewModel.state.value
            assertEquals(expected = false, actual = state.isHourlyForecastEnabled)
            assertEquals(expected = ForecastView.DAYS, actual = state.forecastView)
        }

    @Test
    fun `hours cannot be picked while the hourly forecast is off and can again once it is on`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnForecastViewSelected(forecastView = ForecastView.HOURS))
            advanceUntilIdle()
            val viewWhileOff: ForecastView = weatherForecastViewModel.state.value.forecastView

            forecastPreferenceRepository.setHourlyForecastEnabled(isEnabled = true)
            advanceUntilIdle()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnForecastViewSelected(forecastView = ForecastView.HOURS))
            advanceUntilIdle()

            assertEquals(expected = ForecastView.DAYS, actual = viewWhileOff)
            assertEquals(expected = true, actual = weatherForecastViewModel.state.value.isHourlyForecastEnabled)
            assertEquals(expected = ForecastView.HOURS, actual = weatherForecastViewModel.state.value.forecastView)
        }

    @Test
    fun `a toggled day opens and closes again`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnDayToggled(dayKey = "coming-day-2026-10-24"))
            advanceUntilIdle()
            assertEquals(expected = listOf(true), actual = expansion())

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnDayToggled(dayKey = "coming-day-2026-10-24"))
            advanceUntilIdle()
            assertEquals(expected = listOf(false), actual = expansion())
        }

    @Test
    fun `by default opening a day closes the one already open`() =
        testScope.runTest {
            weatherForecastRepository.forecasts = listOf(
                noon,
                noon.copy(
                    validAt = noon.validAt + 1.days,
                    intervalStartedAt = noon.intervalStartedAt + 1.days
                )
            ).asRight()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnDayToggled(dayKey = "coming-day-2026-10-24"))
            advanceUntilIdle()
            val firstOpened: List<Boolean> = expansion()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnDayToggled(dayKey = "coming-day-2026-10-25"))
            advanceUntilIdle()

            assertEquals(expected = listOf(true, false), actual = firstOpened)
            assertEquals(expected = listOf(false, true), actual = expansion())
        }

    @Test
    fun `with expand once off several days stay open`() =
        testScope.runTest {
            weatherForecastRepository.forecasts = listOf(
                noon,
                noon.copy(
                    validAt = noon.validAt + 1.days,
                    intervalStartedAt = noon.intervalStartedAt + 1.days
                )
            ).asRight()
            forecastPreferenceRepository.setExpandOnceEnabled(isEnabled = false)
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnDayToggled(dayKey = "coming-day-2026-10-24"))
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnDayToggled(dayKey = "coming-day-2026-10-25"))
            advanceUntilIdle()

            assertEquals(expected = listOf(true, true), actual = expansion())
        }

    @Test
    fun `toggling a day without a forecast opens nothing`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnDayToggled(dayKey = "coming-day-2026-10-24"))
            advanceUntilIdle()

            assertEquals(expected = emptyList(), actual = expansion())
        }

    @Test
    fun `a refresh keeps the open days`() =
        testScope.runTest {
            weatherForecastRepository.forecasts = threeDays.asRight()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnDayToggled(dayKey = "coming-day-2026-10-25"))
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnRefresh)
            advanceUntilIdle()

            assertEquals(expected = 2, actual = weatherForecastRepository.requestedCoordinates.size)
            assertEquals(expected = listOf(false, true, false), actual = expansion())
        }

    @Test
    fun `another place starts with every day closed`() =
        testScope.runTest {
            favoriteRepository.favorites.value = listOf(kirunaFavorite)
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnDayToggled(dayKey = "coming-day-2026-10-24"))
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnFavoriteClicked(favorite = kirunaFavorite))
            advanceUntilIdle()

            assertEquals(expected = "Kiruna", actual = weatherForecastViewModel.state.value.localityName)
            assertEquals(expected = listOf(false), actual = expansion())
        }

    @Test
    fun `a new language keeps the open days`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnLocaleChanged(languageTag = "en"))
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnDayToggled(dayKey = "coming-day-2026-10-24"))
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnLocaleChanged(languageTag = "sv-SE"))
            advanceUntilIdle()

            assertEquals(
                expected = "Lör",
                actual = weatherForecastViewModel.state.value.forecast.comingDays.single().weekdayText
            )
            assertEquals(expected = listOf(true), actual = expansion())
        }

    private fun expansion(): List<Boolean> =
        weatherForecastViewModel.state.value.forecast.comingDays.map { comingDay: ComingDay ->
            comingDay.isExpanded
        }
}