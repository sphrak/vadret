package se.vadretapp.vadret.weatherforecast

import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.AddFavoriteTask
import se.vadretapp.vadret.followedplace.SelectFavoriteTask
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.nominatim.model.Locality
import se.vadretapp.vadret.testing.FixedClock
import se.vadretapp.vadret.ui.extension.messageResource
import se.vadretapp.vadret.weatherforecast.model.PlaceNaming
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceIslandViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val weatherForecastRepository = FakeWeatherForecastRepository()
    private val nominatimRepository = FakeNominatimRepository()
    private val favoriteRepository = FakeFavoriteRepository()
    private val locationRepository = FakeLocationRepository()
    private val placeIslandViewModel = PlaceIslandViewModel(
        observePlaceForecastTask = DefaultObservePlaceForecastTask(
            weatherForecastRepository = weatherForecastRepository,
            nominatimRepository = nominatimRepository
        ),
        addFavoriteTask = AddFavoriteTask(
            favoriteRepository = favoriteRepository,
            locationRepository = locationRepository
        ),
        selectFavoriteTask = SelectFavoriteTask(locationRepository = locationRepository),
        clock = FixedClock(now = Instant.parse(input = "2026-10-24T12:00:00Z")),
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

    private fun drop(location: Location) {
        placeIslandViewModel.onEvent(
            event = PlaceIslandEvent.OnPlaceDropped(
                latitude = location.latitude,
                longitude = location.longitude
            )
        )
    }

    @Test
    fun `a dropped place shows its name and the weather there`() =
        testScope.runTest {
            drop(location = gothenburgLocation)
            val loading: PlaceIslandState = placeIslandViewModel.state.value
            advanceUntilIdle()

            val state: PlaceIslandState = placeIslandViewModel.state.value
            assertEquals(expected = true, actual = loading.isLoading)
            assertEquals(expected = gothenburgLocation, actual = state.location)
            assertEquals(expected = "Göteborg", actual = state.placeName)
            assertEquals(expected = "Göteborgs kommun", actual = state.municipality)
            assertEquals(expected = "4", actual = state.currentWeather?.temperatureText)
            assertEquals(expected = false, actual = state.isLoading)
        }

    @Test
    fun `a named place is saved as a favorite without becoming the chosen location`() =
        testScope.runTest {
            drop(location = gothenburgLocation)
            advanceUntilIdle()

            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnSaveClicked)
            advanceUntilIdle()

            assertEquals(
                expected = listOf(
                    Favorite(
                        favoriteId = "favorite-1",
                        locality = "Göteborg",
                        municipality = "Göteborgs kommun",
                        county = "Västra Götalands län",
                        latitude = gothenburgLocation.latitude,
                        longitude = gothenburgLocation.longitude
                    )
                ),
                actual = favoriteRepository.favorites.value
            )
            assertTrue(actual = locationRepository.writes.isEmpty())
            assertEquals(expected = true, actual = placeIslandViewModel.state.value.isSaved)
        }

    @Test
    fun `a place Nominatim cannot name asks for a name when saved and is only saved once it has one`() =
        testScope.runTest {
            nominatimRepository.locality = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()
            drop(location = gothenburgLocation)
            advanceUntilIdle()
            val unnamed: PlaceIslandState = placeIslandViewModel.state.value

            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnSaveClicked)
            val asked: PlaceIslandState = placeIslandViewModel.state.value
            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnLabelChanged(label = "   "))
            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnSaveClicked)
            advanceUntilIdle()
            val blankLabel: PlaceIslandState = placeIslandViewModel.state.value
            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnLabelChanged(label = " Stugan "))
            val named: PlaceIslandState = placeIslandViewModel.state.value
            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnSaveClicked)
            advanceUntilIdle()

            assertNull(actual = unnamed.placeNaming)
            assertEquals(expected = PlaceNaming(), actual = asked.placeNaming)
            assertEquals(expected = true, actual = blankLabel.placeNaming?.isLabelMissing)
            assertEquals(expected = false, actual = blankLabel.isSaved)
            assertEquals(expected = false, actual = named.placeNaming?.isLabelMissing)
            assertEquals(
                expected = listOf(
                    Favorite(
                        favoriteId = "favorite-1",
                        locality = "Stugan",
                        municipality = "",
                        county = "",
                        latitude = gothenburgLocation.latitude,
                        longitude = gothenburgLocation.longitude
                    )
                ),
                actual = favoriteRepository.favorites.value
            )
            assertNull(actual = placeIslandViewModel.state.value.placeNaming)
            assertEquals(expected = "Stugan", actual = placeIslandViewModel.state.value.shownPlaceName)
            assertEquals(expected = true, actual = placeIslandViewModel.state.value.isSaved)
        }

    @Test
    fun `a place carrying the name of a favorite elsewhere asks for a name of its own`() =
        testScope.runTest {
            val gothenburgCenter = Favorite(
                favoriteId = "favorite-gothenburg",
                locality = "Göteborg",
                municipality = "Göteborgs kommun",
                county = "Västra Götalands län",
                latitude = 57.7000,
                longitude = 11.9700
            )
            favoriteRepository.favorites.value = listOf(gothenburgCenter)
            drop(location = gothenburgLocation)
            advanceUntilIdle()

            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnSaveClicked)
            advanceUntilIdle()
            val taken: PlaceIslandState = placeIslandViewModel.state.value
            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnLabelChanged(label = "Hamnen"))
            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnSaveClicked)
            advanceUntilIdle()

            assertEquals(expected = PlaceNaming(takenPlaceName = "Göteborg"), actual = taken.placeNaming)
            assertEquals(expected = false, actual = taken.isSaved)
            assertEquals(
                expected = listOf(
                    gothenburgCenter,
                    Favorite(
                        favoriteId = "favorite-1",
                        locality = "Hamnen",
                        municipality = "Göteborgs kommun",
                        county = "Västra Götalands län",
                        latitude = gothenburgLocation.latitude,
                        longitude = gothenburgLocation.longitude
                    )
                ),
                actual = favoriteRepository.favorites.value
            )
            assertEquals(expected = "Hamnen", actual = placeIslandViewModel.state.value.shownPlaceName)
            assertNull(actual = placeIslandViewModel.state.value.placeNaming)
        }

    @Test
    fun `dismissing the name of a place saves nothing`() =
        testScope.runTest {
            nominatimRepository.locality = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()
            drop(location = gothenburgLocation)
            advanceUntilIdle()

            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnSaveClicked)
            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnLabelChanged(label = "Stu"))
            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnLabelDismissed)
            advanceUntilIdle()

            assertNull(actual = placeIslandViewModel.state.value.placeNaming)
            assertTrue(actual = favoriteRepository.favorites.value.isEmpty())
        }

    @Test
    fun `a favorite that cannot be saved says why`() =
        testScope.runTest {
            val failure = Failure(errorCode = ErrorCode.UNKNOWN)
            favoriteRepository.failure = failure
            drop(location = gothenburgLocation)
            advanceUntilIdle()

            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnSaveClicked)
            advanceUntilIdle()

            val state: PlaceIslandState = placeIslandViewModel.state.value
            assertEquals(expected = failure.messageResource(), actual = state.errorMessageResource)
            assertEquals(expected = false, actual = state.isSaved)
        }

    @Test
    fun `weather that cannot be fetched says why and can be tried again`() =
        testScope.runTest {
            val failure = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE)
            weatherForecastRepository.forecasts = failure.asLeft()
            drop(location = gothenburgLocation)
            advanceUntilIdle()
            val failed: PlaceIslandState = placeIslandViewModel.state.value

            weatherForecastRepository.forecasts = listOf(noon).asRight()
            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnRetryClicked)
            advanceUntilIdle()

            assertNull(actual = failed.currentWeather)
            assertEquals(expected = false, actual = failed.isLoading)
            assertEquals(expected = failure.messageResource(), actual = failed.errorMessageResource)
            assertEquals(expected = "4", actual = placeIslandViewModel.state.value.currentWeather?.temperatureText)
            assertNull(actual = placeIslandViewModel.state.value.errorMessageResource)
        }

    @Test
    fun `a place dropped while another loads replaces it`() =
        testScope.runTest {
            val gothenburgForecast: CompletableDeferred<Unit> = CompletableDeferred()
            weatherForecastRepository.forecastGates[gothenburgLocation.latitude to gothenburgLocation.longitude] = gothenburgForecast
            drop(location = gothenburgLocation)
            advanceUntilIdle()

            nominatimRepository.locality = Locality(
                name = "Kiruna",
                municipality = "Kiruna kommun",
                county = "Norrbottens län"
            ).asRight()
            drop(location = kirunaLocation)
            advanceUntilIdle()
            gothenburgForecast.complete(value = Unit)
            advanceUntilIdle()

            val state: PlaceIslandState = placeIslandViewModel.state.value
            assertEquals(expected = kirunaLocation, actual = state.location)
            assertEquals(expected = "Kiruna", actual = state.placeName)
        }

    @Test
    fun `the same drop shown again keeps what the island already has`() =
        testScope.runTest {
            drop(location = gothenburgLocation)
            advanceUntilIdle()
            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnSaveClicked)
            advanceUntilIdle()

            drop(location = gothenburgLocation)
            advanceUntilIdle()

            assertEquals(expected = true, actual = placeIslandViewModel.state.value.isSaved)
            assertEquals(expected = listOf(gothenburgLocation.latitude to gothenburgLocation.longitude), actual = weatherForecastRepository.requestedCoordinates)
        }

    @Test
    fun `opening a place not saved opens its own forecast and keeps the island`() =
        testScope.runTest {
            drop(location = gothenburgLocation)
            advanceUntilIdle()

            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnOpenForecastClicked)
            val opening: PlaceIslandState = placeIslandViewModel.state.value
            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnOpeningShown)

            val state: PlaceIslandState = placeIslandViewModel.state.value
            assertEquals(expected = PlaceOpening.PLACE_FORECAST, actual = opening.opening)
            assertNull(actual = state.opening)
            assertEquals(expected = gothenburgLocation, actual = state.location)
            assertEquals(expected = "Göteborg", actual = state.placeName)
            assertTrue(actual = locationRepository.writes.isEmpty())
        }

    @Test
    fun `opening a saved place chooses it and leaves the island empty for the next drop`() =
        testScope.runTest {
            drop(location = gothenburgLocation)
            advanceUntilIdle()
            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnSaveClicked)
            advanceUntilIdle()

            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnOpenForecastClicked)
            advanceUntilIdle()
            val opening: PlaceIslandState = placeIslandViewModel.state.value
            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnOpeningShown)
            val forgotten: PlaceIslandState = placeIslandViewModel.state.value
            drop(location = gothenburgLocation)
            advanceUntilIdle()

            assertEquals(expected = PlaceOpening.CHOSEN_LOCATION, actual = opening.opening)
            assertEquals(expected = listOf("selected=Göteborg"), actual = locationRepository.writes)
            assertEquals(expected = PlaceIslandState(), actual = forgotten)
            assertEquals(
                expected = listOf(
                    gothenburgLocation.latitude to gothenburgLocation.longitude,
                    gothenburgLocation.latitude to gothenburgLocation.longitude
                ),
                actual = weatherForecastRepository.requestedCoordinates
            )
        }

    @Test
    fun `a saved place that cannot be chosen says why and stays on the island`() =
        testScope.runTest {
            val failure = Failure(errorCode = ErrorCode.UNKNOWN)
            locationRepository.selectStoredLocationResult = failure.asLeft()
            drop(location = gothenburgLocation)
            advanceUntilIdle()
            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnSaveClicked)
            advanceUntilIdle()

            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnOpenForecastClicked)
            advanceUntilIdle()

            val state: PlaceIslandState = placeIslandViewModel.state.value
            assertEquals(expected = failure.messageResource(), actual = state.errorMessageResource)
            assertNull(actual = state.opening)
            assertEquals(expected = true, actual = state.isSaved)
        }

    @Test
    fun `opening before anything was dropped opens nothing`() =
        testScope.runTest {
            placeIslandViewModel.onEvent(event = PlaceIslandEvent.OnOpenForecastClicked)
            advanceUntilIdle()

            assertNull(actual = placeIslandViewModel.state.value.opening)
        }
}

private val gothenburgLocation = Location(
    latitude = 57.7089,
    longitude = 11.9746
)
private val kirunaLocation = Location(
    latitude = 67.8558,
    longitude = 20.2253
)