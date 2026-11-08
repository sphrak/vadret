package se.vadretapp.vadret.weatherforecast

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
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.SearchPlacesTask
import se.vadretapp.vadret.followedplace.SelectPlaceTask
import se.vadretapp.vadret.location.model.StoredLocation
import se.vadretapp.vadret.ui.extension.messageResource
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.followed_places_limit_text
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceSearchViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val locationRepository = FakeLocationRepository()
    private val placeRepository = FakePlaceRepository()
    private val favoriteRepository = FakeFavoriteRepository()
    private val placeSearchViewModel = PlaceSearchViewModel(
        searchPlacesTask = SearchPlacesTask(placeRepository = placeRepository),
        selectPlaceTask = SelectPlaceTask(
            favoriteRepository = favoriteRepository,
            locationRepository = locationRepository
        ),
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

    private fun chosenLocation(): StoredLocation? = locationRepository.locationPreference.value.storedLocation

    @Test
    fun `typing searches once it pauses and keeps only the latest query`() =
        testScope.runTest {
            placeSearchViewModel.onEvent(event = PlaceSearchEvent.OnQueryChanged(query = "K"))
            advanceTimeBy(delayTimeMillis = 30)
            placeSearchViewModel.onEvent(event = PlaceSearchEvent.OnQueryChanged(query = "Ki"))
            advanceUntilIdle()

            assertEquals(expected = listOf("Ki" to 10), actual = placeRepository.searches)
            assertEquals(
                expected = PlaceSearchState(
                    query = "Ki",
                    suggestions = persistentListOf(kiruna)
                ),
                actual = placeSearchViewModel.state.value
            )
        }

    @Test
    fun `a blank query clears the suggestions without searching`() =
        testScope.runTest {
            placeSearchViewModel.onEvent(event = PlaceSearchEvent.OnQueryChanged(query = "Ki"))
            advanceUntilIdle()
            placeSearchViewModel.onEvent(event = PlaceSearchEvent.OnQueryChanged(query = " "))
            advanceUntilIdle()

            assertEquals(expected = listOf("Ki" to 10), actual = placeRepository.searches)
            assertEquals(
                expected = PlaceSearchState(query = " "),
                actual = placeSearchViewModel.state.value
            )
        }

    @Test
    fun `a picked suggestion becomes the chosen location and closes the search`() =
        testScope.runTest {
            placeSearchViewModel.onEvent(event = PlaceSearchEvent.OnSuggestionClicked(place = kiruna))
            advanceUntilIdle()

            assertEquals(expected = true, actual = placeSearchViewModel.state.value.isPlaceChosen)
            assertEquals(expected = listOf(kirunaFavorite), actual = favoriteRepository.favorites.value)
            assertEquals(expected = kirunaStoredLocation, actual = chosenLocation())
        }

    @Test
    fun `submitting chooses the first place found for the query as written`() =
        testScope.runTest {
            placeSearchViewModel.onEvent(event = PlaceSearchEvent.OnQueryChanged(query = "Ki"))
            advanceUntilIdle()

            placeSearchViewModel.onEvent(event = PlaceSearchEvent.OnSearchSubmitted(query = "Kir"))
            advanceUntilIdle()

            assertEquals(expected = listOf("Ki" to 10, "Kir" to 10), actual = placeRepository.searches)
            assertEquals(expected = true, actual = placeSearchViewModel.state.value.isPlaceChosen)
            assertEquals(expected = kirunaStoredLocation, actual = chosenLocation())
        }

    @Test
    fun `submitting a query that finds nothing keeps the search open`() =
        testScope.runTest {
            placeSearchViewModel.onEvent(event = PlaceSearchEvent.OnSearchSubmitted(query = "Ystad"))
            advanceUntilIdle()

            assertEquals(
                expected = false to emptyList<Favorite>(),
                actual = placeSearchViewModel.state.value.isPlaceChosen to favoriteRepository.favorites.value
            )
        }

    @Test
    fun `a sixth saved place is refused and says why while the search stays open`() =
        testScope.runTest {
            favoriteRepository.favorites.value = (1..5).map { number: Int ->
                kirunaFavorite.copy(
                    favoriteId = "favorite-$number",
                    locality = "Plats $number"
                )
            }

            placeSearchViewModel.onEvent(event = PlaceSearchEvent.OnSuggestionClicked(place = kiruna))
            advanceUntilIdle()

            assertEquals(
                expected = Triple(Res.string.followed_places_limit_text, false, 5),
                actual = Triple(
                    placeSearchViewModel.state.value.errorMessageResource,
                    placeSearchViewModel.state.value.isPlaceChosen,
                    favoriteRepository.favorites.value.size
                )
            )
        }

    @Test
    fun `a failed search says why until the message is seen`() =
        testScope.runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            placeRepository.failure = failure

            placeSearchViewModel.onEvent(event = PlaceSearchEvent.OnQueryChanged(query = "Ki"))
            advanceUntilIdle()
            val shownMessage = placeSearchViewModel.state.value.errorMessageResource
            placeSearchViewModel.onEvent(event = PlaceSearchEvent.OnErrorShown)

            assertEquals(
                expected = failure.messageResource() to null,
                actual = shownMessage to placeSearchViewModel.state.value.errorMessageResource
            )
        }

    @Test
    fun `a failed submit says why and chooses nothing`() =
        testScope.runTest {
            placeRepository.failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)

            placeSearchViewModel.onEvent(event = PlaceSearchEvent.OnSearchSubmitted(query = "Kir"))
            advanceUntilIdle()

            assertEquals(
                expected = Failure(errorCode = ErrorCode.STORAGE_FAILED).messageResource() to false,
                actual = placeSearchViewModel.state.value.errorMessageResource to placeSearchViewModel.state.value.isPlaceChosen
            )
        }
}