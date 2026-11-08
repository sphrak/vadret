package se.vadretapp.vadret.weatherforecast

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
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.FeedSource
import se.vadretapp.vadret.aggregatedfeed.model.WarningLevel
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.AddFavoriteTask
import se.vadretapp.vadret.followedplace.ObserveLocationChoicesTask
import se.vadretapp.vadret.followedplace.RemoveFavoriteTask
import se.vadretapp.vadret.followedplace.SearchPlacesTask
import se.vadretapp.vadret.followedplace.SelectFavoriteTask
import se.vadretapp.vadret.followedplace.SelectPlaceTask
import se.vadretapp.vadret.followedplace.SetLocationModeTask
import se.vadretapp.vadret.followedplace.model.FavoriteChoice
import se.vadretapp.vadret.followedplace.model.LocationChoices
import se.vadretapp.vadret.testing.FixedClock
import se.vadretapp.vadret.ui.extension.messageResource
import se.vadretapp.vadret.ui.format.currentLanguageTag
import se.vadretapp.vadret.ui.format.toDayTimeText
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.data_not_available_at_this_location
import se.vadretapp.vadret.ui.resources.followed_places_limit_text
import se.vadretapp.vadret.ui.resources.unhandled_error
import se.vadretapp.vadret.warning.model.WarningPlace
import se.vadretapp.vadret.weatherforecast.model.ComingDay
import se.vadretapp.vadret.weatherforecast.model.Forecast
import se.vadretapp.vadret.weatherforecast.model.ForecastDay
import se.vadretapp.vadret.weatherforecast.model.ForecastView
import se.vadretapp.vadret.weatherforecast.model.LatestWarning
import se.vadretapp.vadret.weatherforecast.model.PlaceNaming
import se.vadretapp.vadret.weatherforecast.model.SearchState
import se.vadretapp.vadret.weatherforecast.usecase.DefaultGetLocationModeTask
import se.vadretapp.vadret.weatherforecast.usecase.DefaultObserveForecastPreferencesTask
import se.vadretapp.vadret.weatherforecast.usecase.DefaultObservePlaceForecastTask
import se.vadretapp.vadret.weatherforecast.usecase.DefaultObserveWeatherForecastTask
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
    private val weatherForecastRepository = FakeWeatherForecastRepository(now = { clock.now })
    private val placeRepository = FakePlaceRepository()
    private val favoriteRepository = FakeFavoriteRepository()
    private val clock = FixedClock(now = Instant.parse(input = "2026-10-24T12:00:00Z"))
    private val observeUnseenWarningsTask = FakeObserveUnseenWarningsTask()
    private val forecastPreferenceRepository = FakeForecastPreferenceRepository()
    private val nominatimRepository = FakeNominatimRepository()
    private val weatherForecastViewModel = WeatherForecastViewModel(
        getLocationModeTask = DefaultGetLocationModeTask(locationRepository = locationRepository),
        getChosenPlaceTask = FakeGetChosenPlaceTask(
            locationRepository = locationRepository,
            nominatimRepository = FakeNominatimRepository()
        ),
        setLocationModeTask = SetLocationModeTask(
            locationRepository = locationRepository,
            favoriteRepository = favoriteRepository
        ),
        observeWeatherForecastTask = DefaultObserveWeatherForecastTask(
            getChosenPlaceTask = FakeGetChosenPlaceTask(
                locationRepository = locationRepository,
                nominatimRepository = FakeNominatimRepository()
            ),
            weatherForecastRepository = weatherForecastRepository
        ),
        observeLocationChoicesTask = ObserveLocationChoicesTask(
            favoriteRepository = favoriteRepository,
            locationRepository = locationRepository
        ),
        observeForecastPreferencesTask = DefaultObserveForecastPreferencesTask(forecastPreferenceRepository = forecastPreferenceRepository),
        selectFavoriteTask = SelectFavoriteTask(locationRepository = locationRepository),
        removeFavoriteTask = RemoveFavoriteTask(favoriteRepository = favoriteRepository),
        searchPlacesTask = SearchPlacesTask(placeRepository = placeRepository),
        selectPlaceTask = SelectPlaceTask(
            favoriteRepository = favoriteRepository,
            locationRepository = locationRepository
        ),
        observePlaceForecastTask = DefaultObservePlaceForecastTask(
            weatherForecastRepository = weatherForecastRepository,
            nominatimRepository = nominatimRepository
        ),
        addFavoriteTask = AddFavoriteTask(
            favoriteRepository = favoriteRepository,
            locationRepository = locationRepository
        ),
        observeUnseenWarningsTask = observeUnseenWarningsTask,
        observeUnseenFollowedPlaceWarningsTask = observeUnseenWarningsTask,
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
    fun `a refresh that fails keeps the saved forecast and tells when it was fetched until a refresh works`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()
            val beforeFailing: String? = weatherForecastViewModel.state.value.outdatedForecastTimeText
            weatherForecastRepository.forecasts = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnRefresh)
            advanceUntilIdle()
            val afterFailing: WeatherForecastState = weatherForecastViewModel.state.value
            weatherForecastRepository.forecasts = listOf(noon).asRight()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnRefresh)
            advanceUntilIdle()

            assertEquals(
                expected = listOf(
                    null,
                    clock.now.toDayTimeText(
                        now = clock.now,
                        timeZone = TimeZone.currentSystemDefault(),
                        languageTag = currentLanguageTag()
                    ),
                    null
                ),
                actual = listOf(
                    beforeFailing,
                    afterFailing.outdatedForecastTimeText,
                    weatherForecastViewModel.state.value.outdatedForecastTimeText
                )
            )
            assertEquals(
                expected = Triple(
                    first = true,
                    second = null,
                    third = false
                ),
                actual = Triple(
                    first = afterFailing.forecast.days.isNotEmpty(),
                    second = afterFailing.errorMessageResource,
                    third = afterFailing.isRefreshing
                )
            )
        }

    @Test
    fun `a first load that fails says why instead of telling of a saved forecast`() =
        testScope.runTest {
            weatherForecastRepository.forecasts = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            assertEquals(
                expected = null to true,
                actual = weatherForecastViewModel.state.value.outdatedForecastTimeText to (weatherForecastViewModel.state.value.errorMessageResource != null)
            )
        }

    @Test
    fun `the latest unseen warning of the followed places is shown once the forecast is shown`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            assertEquals(
                expected = LatestWarning(
                    warningLevel = WarningLevel.YELLOW,
                    slipperinessRisk = null,
                    source = FeedSource.SMHI,
                    isPublicAlert = false,
                    headline = "Vind",
                    endsAtText = null,
                    warningIdentifiers = persistentListOf("smhi-1", "smhi-2")
                ),
                actual = weatherForecastViewModel.state.value.latestWarning
            )
            assertEquals<List<Pair<WarningPlace?, Boolean>>>(
                expected = listOf(null to false),
                actual = observeUnseenWarningsTask.requests
            )
        }

    @Test
    fun `a dropped place asks for its own warnings by its county and municipality`() =
        testScope.runTest {
            showDroppedPlace()
            advanceUntilIdle()

            assertEquals<List<Pair<WarningPlace?, Boolean>>>(
                expected = listOf(
                    WarningPlace(
                        latitude = DROPPED_LATITUDE,
                        longitude = DROPPED_LONGITUDE,
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
                actual = observeUnseenWarningsTask.requests.map { request: Pair<WarningPlace?, Boolean> -> request.second }
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
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSearchQueryChanged(query = "Ki"))
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSearchSubmitted(query = "Kir"))
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
                    chosenPlaceName = "Kiruna",
                    favorites = persistentListOf(
                        FavoriteChoice(
                            favorite = kirunaFavorite,
                            isSelected = true,
                            isFollowed = true
                        )
                    )
                ),
                actual = weatherForecastViewModel.state.value.locationChoices
            )
        }

    @Test
    fun `a sixth saved place is refused and says why`() =
        testScope.runTest {
            favoriteRepository.favorites.value = (1..5).map { number: Int ->
                kirunaFavorite.copy(
                    favoriteId = "favorite-$number",
                    locality = "Plats $number"
                )
            }
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSuggestionClicked(place = kiruna))
            advanceUntilIdle()

            assertEquals(expected = Res.string.followed_places_limit_text, actual = weatherForecastViewModel.state.value.errorMessageResource)
            assertEquals(expected = 5, actual = favoriteRepository.favorites.value.size)
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

    @Test
    fun `a place chosen while the screen was out of sight is loaded when it is shown again`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()
            locationRepository.selectStoredLocation(storedLocation = kirunaStoredLocation)

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnShown)
            advanceUntilIdle()

            assertEquals(expected = "Kiruna", actual = weatherForecastViewModel.state.value.localityName)
            assertEquals(
                expected = listOf(
                    59.3293 to 18.0686,
                    kirunaFavorite.latitude to kirunaFavorite.longitude
                ),
                actual = weatherForecastRepository.requestedCoordinates
            )
        }

    @Test
    fun `a dropped place shows its own forecast and name instead of the chosen location`() =
        testScope.runTest {
            showDroppedPlace()
            advanceUntilIdle()

            val state: WeatherForecastState = weatherForecastViewModel.state.value
            assertEquals(expected = "Göteborg", actual = state.localityName)
            assertEquals(expected = false, actual = state.isPlaceSaved)
            assertEquals(expected = false, actual = state.isLoading)
            assertTrue(actual = state.forecast.days.isNotEmpty())
            assertEquals(expected = listOf(DROPPED_LATITUDE to DROPPED_LONGITUDE), actual = weatherForecastRepository.requestedCoordinates)
        }

    @Test
    fun `a dropped place shown again is not fetched again`() =
        testScope.runTest {
            showDroppedPlace()
            advanceUntilIdle()
            showDroppedPlace()
            advanceUntilIdle()

            assertEquals(expected = 1, actual = weatherForecastRepository.requestedCoordinates.size)
        }

    @Test
    fun `a refresh fetches the dropped place again`() =
        testScope.runTest {
            showDroppedPlace()
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnRefresh)
            advanceUntilIdle()

            assertEquals(
                expected = listOf(
                    DROPPED_LATITUDE to DROPPED_LONGITUDE,
                    DROPPED_LATITUDE to DROPPED_LONGITUDE
                ),
                actual = weatherForecastRepository.requestedCoordinates
            )
            assertEquals(expected = false, actual = weatherForecastViewModel.state.value.isRefreshing)
        }

    @Test
    fun `a dropped place is saved as a favorite and then says so`() =
        testScope.runTest {
            showDroppedPlace()
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSavePlaceClicked)
            advanceUntilIdle()

            assertEquals(
                expected = listOf(
                    Favorite(
                        favoriteId = "favorite-1",
                        locality = "Göteborg",
                        municipality = "Göteborgs kommun",
                        county = "Västra Götalands län",
                        latitude = DROPPED_LATITUDE,
                        longitude = DROPPED_LONGITUDE
                    )
                ),
                actual = favoriteRepository.favorites.value
            )
            assertEquals(expected = true, actual = weatherForecastViewModel.state.value.isPlaceSaved)
        }

    @Test
    fun `a dropped place carrying the name of a favorite elsewhere is not saved and asks for a name of its own`() =
        testScope.runTest {
            favoriteRepository.favorites.value = listOf(
                Favorite(
                    favoriteId = "favorite-gothenburg",
                    locality = "Göteborg",
                    municipality = "Göteborgs kommun",
                    county = "Västra Götalands län",
                    latitude = 57.7089,
                    longitude = 11.9746
                )
            )
            showDroppedPlace()
            advanceUntilIdle()
            val shown: WeatherForecastState = weatherForecastViewModel.state.value

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSavePlaceClicked)
            advanceUntilIdle()
            val taken: WeatherForecastState = weatherForecastViewModel.state.value
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnPlaceLabelChanged(label = "Hamnen"))
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSavePlaceClicked)
            advanceUntilIdle()

            assertEquals(expected = false, actual = shown.isPlaceSaved)
            assertEquals(expected = PlaceNaming(takenPlaceName = "Göteborg"), actual = taken.placeNaming)
            assertEquals(expected = false, actual = taken.isPlaceSaved)
            assertEquals(
                expected = listOf("Göteborg", "Hamnen"),
                actual = favoriteRepository.favorites.value.map { favorite: Favorite -> favorite.locality }
            )
            val state: WeatherForecastState = weatherForecastViewModel.state.value
            assertNull(actual = state.placeNaming)
            assertEquals(expected = "Hamnen", actual = state.localityName)
            assertEquals(expected = true, actual = state.isPlaceSaved)
        }

    @Test
    fun `an unnamed dropped place saved before goes by the name it was saved with`() =
        testScope.runTest {
            nominatimRepository.locality = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()
            favoriteRepository.favorites.value = listOf(
                Favorite(
                    favoriteId = "favorite-cabin",
                    locality = "Stugan",
                    municipality = "",
                    county = "",
                    latitude = DROPPED_LATITUDE,
                    longitude = DROPPED_LONGITUDE
                )
            )

            showDroppedPlace()
            advanceUntilIdle()

            assertEquals(expected = "Stugan", actual = weatherForecastViewModel.state.value.localityName)
            assertEquals(expected = true, actual = weatherForecastViewModel.state.value.isPlaceSaved)
        }

    @Test
    fun `a dropped place Nominatim cannot name asks for a name before it is saved`() =
        testScope.runTest {
            nominatimRepository.locality = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()
            showDroppedPlace()
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSavePlaceClicked)
            val asked: WeatherForecastState = weatherForecastViewModel.state.value
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSavePlaceClicked)
            val blank: WeatherForecastState = weatherForecastViewModel.state.value
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnPlaceLabelChanged(label = " Stugan "))
            val named: WeatherForecastState = weatherForecastViewModel.state.value
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSavePlaceClicked)
            advanceUntilIdle()

            assertNull(actual = asked.localityName)
            assertEquals(expected = PlaceNaming(), actual = asked.placeNaming)
            assertEquals(expected = true, actual = blank.placeNaming?.isLabelMissing)
            assertEquals(expected = false, actual = named.placeNaming?.isLabelMissing)
            assertEquals(
                expected = listOf(
                    Favorite(
                        favoriteId = "favorite-1",
                        locality = "Stugan",
                        municipality = "",
                        county = "",
                        latitude = DROPPED_LATITUDE,
                        longitude = DROPPED_LONGITUDE
                    )
                ),
                actual = favoriteRepository.favorites.value
            )
            val state: WeatherForecastState = weatherForecastViewModel.state.value
            assertNull(actual = state.placeNaming)
            assertEquals(expected = "Stugan", actual = state.localityName)
            assertEquals(expected = true, actual = state.isPlaceSaved)
        }

    @Test
    fun `dismissing the name of a dropped place saves nothing`() =
        testScope.runTest {
            nominatimRepository.locality = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()
            showDroppedPlace()
            advanceUntilIdle()

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSavePlaceClicked)
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnPlaceLabelChanged(label = "Stu"))
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnPlaceLabelDismissed)
            advanceUntilIdle()

            assertNull(actual = weatherForecastViewModel.state.value.placeNaming)
            assertTrue(actual = favoriteRepository.favorites.value.isEmpty())
        }

    @Test
    fun `a dropped place that cannot be saved says why`() =
        testScope.runTest {
            showDroppedPlace()
            advanceUntilIdle()
            val failure = Failure(errorCode = ErrorCode.UNKNOWN)
            favoriteRepository.failure = failure

            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSavePlaceClicked)
            advanceUntilIdle()

            assertEquals(expected = failure.messageResource(), actual = weatherForecastViewModel.state.value.errorMessageResource)
            assertEquals(expected = false, actual = weatherForecastViewModel.state.value.isPlaceSaved)
        }

    @Test
    fun `saving before the dropped place has loaded does nothing`() =
        testScope.runTest {
            weatherForecastViewModel.onEvent(event = WeatherForecastEvent.OnSavePlaceClicked)
            advanceUntilIdle()

            assertNull(actual = weatherForecastViewModel.state.value.placeNaming)
            assertTrue(actual = favoriteRepository.favorites.value.isEmpty())
        }

    private fun showDroppedPlace() {
        weatherForecastViewModel.onEvent(
            event = WeatherForecastEvent.OnPlaceShown(
                latitude = DROPPED_LATITUDE,
                longitude = DROPPED_LONGITUDE
            )
        )
    }

    private fun expansion(): List<Boolean> =
        weatherForecastViewModel.state.value.forecast.comingDays.map { comingDay: ComingDay ->
            comingDay.isExpanded
        }
}

private const val DROPPED_LATITUDE = 61.8286
private const val DROPPED_LONGITUDE = 16.0912