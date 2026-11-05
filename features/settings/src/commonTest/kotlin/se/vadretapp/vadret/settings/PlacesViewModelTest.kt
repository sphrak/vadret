package se.vadretapp.vadret.settings

import kotlinx.collections.immutable.persistentListOf
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
import se.vadretapp.vadret.followedplace.ObserveLocationChoicesTask
import se.vadretapp.vadret.followedplace.RemoveFavoriteTask
import se.vadretapp.vadret.followedplace.SearchPlacesTask
import se.vadretapp.vadret.followedplace.SelectFavoriteTask
import se.vadretapp.vadret.followedplace.SetLocationModeTask
import se.vadretapp.vadret.followedplace.model.FavoriteChoice
import se.vadretapp.vadret.place.model.Place
import se.vadretapp.vadret.settings.model.FailureDialog
import se.vadretapp.vadret.ui.resources.Res
import se.vadretapp.vadret.ui.resources.followed_places_limit
import se.vadretapp.vadret.ui.resources.followed_places_limit_text
import se.vadretapp.vadret.ui.resources.no_location_permission
import se.vadretapp.vadret.ui.resources.settings_places
import se.vadretapp.vadret.ui.resources.unhandled_error
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val storageFailureDialog = FailureDialog(
    titleResource = Res.string.settings_places,
    textResource = Res.string.unhandled_error
)

private val placeLimitDialog = FailureDialog(
    titleResource = Res.string.followed_places_limit,
    textResource = Res.string.followed_places_limit_text
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlacesViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(context = testDispatcher)
    private val locationRepository = FakeLocationRepository()
    private val favoriteRepository = FakeFavoriteRepository()
    private val placeRepository = FakePlaceRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher = testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): PlacesViewModel {
        val placesViewModel = PlacesViewModel(
            observeLocationChoicesTask = ObserveLocationChoicesTask(
                favoriteRepository = favoriteRepository,
                locationRepository = locationRepository
            ),
            setLocationModeTask = SetLocationModeTask(
                locationRepository = locationRepository,
                favoriteRepository = favoriteRepository
            ),
            selectFavoriteTask = SelectFavoriteTask(locationRepository = locationRepository),
            removeFavoriteTask = RemoveFavoriteTask(favoriteRepository = favoriteRepository),
            searchPlacesTask = SearchPlacesTask(placeRepository = placeRepository),
            addFavoriteTask = AddFavoriteTask(
                favoriteRepository = favoriteRepository,
                locationRepository = locationRepository
            ),
            coroutineDispatcher = testDispatcher
        )
        placesViewModel.onEvent(event = PlacesEvent.OnStart)
        return placesViewModel
    }

    private fun PlacesViewModel.shownPlaces(): List<Pair<String, Boolean>> =
        state.value.favoriteChoices.map { favoriteChoice: FavoriteChoice -> favoriteChoice.favorite.locality to favoriteChoice.isSelected }

    @Test
    fun `the saved places are listed with the chosen one marked`() =
        testScope.runTest {
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                expected = listOf("Umeå" to true, "Visby" to false),
                actual = placesViewModel.shownPlaces()
            )
        }

    @Test
    fun `following the position is stored and marks no saved place`() =
        testScope.runTest {
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()

            placesViewModel.onEvent(event = PlacesEvent.OnFollowPositionToggled(isFollowingPosition = true))
            advanceUntilIdle()
            val isPermissionRequested: Boolean = placesViewModel.state.value.isLocationPermissionRequested
            placesViewModel.onEvent(event = PlacesEvent.OnLocationPermissionResult(isGranted = true))
            advanceUntilIdle()

            assertEquals(
                expected = listOf(true, false, true),
                actual = listOf(
                    isPermissionRequested,
                    placesViewModel.state.value.isLocationPermissionRequested,
                    placesViewModel.state.value.isFollowingPosition
                )
            )
            assertEquals(
                expected = listOf("Umeå" to false, "Visby" to false),
                actual = placesViewModel.shownPlaces()
            )
        }

    @Test
    fun `a denied location permission keeps the chosen place and says why`() =
        testScope.runTest {
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()

            placesViewModel.onEvent(event = PlacesEvent.OnFollowPositionToggled(isFollowingPosition = true))
            placesViewModel.onEvent(event = PlacesEvent.OnLocationPermissionResult(isGranted = false))
            advanceUntilIdle()

            assertEquals(
                expected = false to FailureDialog(
                    titleResource = Res.string.settings_places,
                    textResource = Res.string.no_location_permission
                ),
                actual = locationRepository.locationPreference.value.isAutomatic to placesViewModel.state.value.failureDialog
            )
            assertEquals(expected = listOf("Umeå" to true, "Visby" to false), actual = placesViewModel.shownPlaces())
        }

    @Test
    fun `turning the position off goes back to the chosen place without asking`() =
        testScope.runTest {
            locationRepository.setAutomatic(isAutomatic = true)
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()

            placesViewModel.onEvent(event = PlacesEvent.OnFollowPositionToggled(isFollowingPosition = false))
            advanceUntilIdle()

            assertEquals(
                expected = listOf(false, false),
                actual = listOf(
                    placesViewModel.state.value.isLocationPermissionRequested,
                    placesViewModel.state.value.isFollowingPosition
                )
            )
            assertEquals(expected = listOf("Umeå" to true, "Visby" to false), actual = placesViewModel.shownPlaces())
        }

    @Test
    fun `choosing a saved place makes it the chosen one`() =
        testScope.runTest {
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()

            placesViewModel.onEvent(event = PlacesEvent.OnPlaceChosen(favorite = visby))
            advanceUntilIdle()

            assertEquals(
                expected = listOf("Umeå" to false, "Visby" to true),
                actual = placesViewModel.shownPlaces()
            )
        }

    @Test
    fun `a removed place is no longer listed`() =
        testScope.runTest {
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()

            placesViewModel.onEvent(event = PlacesEvent.OnPlaceRemoved(favoriteId = visby.favoriteId))
            advanceUntilIdle()

            assertEquals(expected = listOf("Umeå" to true), actual = placesViewModel.shownPlaces())
        }

    @Test
    fun `a search shows the places it finds and a blank one shows none`() =
        testScope.runTest {
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()

            placesViewModel.onEvent(event = PlacesEvent.OnSearchQueryChanged(query = "Ki"))
            advanceUntilIdle()
            val found: List<Place> = placesViewModel.state.value.suggestions
            placesViewModel.onEvent(event = PlacesEvent.OnSearchQueryChanged(query = " "))
            advanceUntilIdle()

            assertEquals(
                expected = listOf(listOf(kiruna), emptyList()),
                actual = listOf(found, placesViewModel.state.value.suggestions)
            )
            assertEquals(expected = listOf("Ki"), actual = placeRepository.searches)
        }

    @Test
    fun `only the last of quick searches asks for places`() =
        testScope.runTest {
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()

            placesViewModel.onEvent(event = PlacesEvent.OnSearchQueryChanged(query = "K"))
            placesViewModel.onEvent(event = PlacesEvent.OnSearchQueryChanged(query = "Ka"))
            advanceUntilIdle()

            assertEquals(
                expected = listOf("Ka") to listOf(kalmar),
                actual = placeRepository.searches to placesViewModel.state.value.suggestions
            )
        }

    @Test
    fun `clearing the search empties it`() =
        testScope.runTest {
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()
            placesViewModel.onEvent(event = PlacesEvent.OnSearchQueryChanged(query = "Ki"))
            advanceUntilIdle()

            placesViewModel.onEvent(event = PlacesEvent.OnSearchCleared)
            advanceUntilIdle()

            assertEquals(
                expected = "" to persistentListOf(),
                actual = placesViewModel.state.value.searchQuery to placesViewModel.state.value.suggestions
            )
        }

    @Test
    fun `a place picked from the search is saved without becoming the chosen one`() =
        testScope.runTest {
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()
            placesViewModel.onEvent(event = PlacesEvent.OnSearchQueryChanged(query = "Ki"))
            advanceUntilIdle()

            placesViewModel.onEvent(event = PlacesEvent.OnSuggestionClicked(place = kiruna))
            advanceUntilIdle()

            assertEquals(
                expected = listOf("Umeå" to true, "Visby" to false, "Kiruna" to false),
                actual = placesViewModel.shownPlaces()
            )
            assertEquals(expected = "", actual = placesViewModel.state.value.searchQuery)
        }

    @Test
    fun `submitting the search saves the first place it found`() =
        testScope.runTest {
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()
            placesViewModel.onEvent(event = PlacesEvent.OnSearchQueryChanged(query = "Ka"))
            advanceUntilIdle()

            placesViewModel.onEvent(event = PlacesEvent.OnSearchSubmitted(query = "Ka"))
            advanceUntilIdle()

            assertEquals(expected = "Kalmar", actual = favoriteRepository.favorites.value.last().locality)
        }

    @Test
    fun `submitting a search that found nothing saves nothing`() =
        testScope.runTest {
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()

            placesViewModel.onEvent(event = PlacesEvent.OnSearchSubmitted(query = "Xyz"))
            advanceUntilIdle()

            assertEquals(expected = listOf(umea, visby), actual = favoriteRepository.favorites.value)
        }

    @Test
    fun `a sixth place is refused and says why until the notice is dismissed`() =
        testScope.runTest {
            favoriteRepository.favorites.value = (1..5).map { number: Int ->
                umea.copy(
                    favoriteId = "favorite-$number",
                    locality = "Plats $number"
                )
            }
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()

            placesViewModel.onEvent(event = PlacesEvent.OnSuggestionClicked(place = kiruna))
            advanceUntilIdle()
            val failureDialog: FailureDialog? = placesViewModel.state.value.failureDialog
            placesViewModel.onEvent(event = PlacesEvent.OnFailureDismissed)

            assertEquals(expected = placeLimitDialog, actual = failureDialog)
            assertNull(actual = placesViewModel.state.value.failureDialog)
            assertEquals(expected = 5, actual = favoriteRepository.favorites.value.size)
        }

    @Test
    fun `following the position next to five saved places is refused and says why`() =
        testScope.runTest {
            favoriteRepository.favorites.value = (1..5).map { number: Int ->
                umea.copy(
                    favoriteId = "favorite-$number",
                    locality = "Plats $number"
                )
            }
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()

            placesViewModel.onEvent(event = PlacesEvent.OnFollowPositionToggled(isFollowingPosition = true))
            placesViewModel.onEvent(event = PlacesEvent.OnLocationPermissionResult(isGranted = true))
            advanceUntilIdle()

            assertEquals(
                expected = false to placeLimitDialog,
                actual = placesViewModel.state.value.isFollowingPosition to placesViewModel.state.value.failureDialog
            )
        }

    @Test
    fun `saved places past the limit are told apart`() =
        testScope.runTest {
            favoriteRepository.favorites.value = (1..6).map { number: Int ->
                umea.copy(
                    favoriteId = "favorite-$number",
                    locality = "Plats $number"
                )
            }
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                expected = listOf(true, true, true, true, false, false),
                actual = placesViewModel.state.value.favoriteChoices.map { favoriteChoice: FavoriteChoice -> favoriteChoice.isFollowed }
            )
        }

    @Test
    fun `a searched place named like a saved one is that place and says nothing`() =
        testScope.runTest {
            favoriteRepository.favorites.value = listOf(
                umea,
                kiruna.toFavorite(favoriteId = "favorite-dropped-pin").copy(latitude = 68.0)
            )
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()

            placesViewModel.onEvent(event = PlacesEvent.OnSuggestionClicked(place = kiruna))
            advanceUntilIdle()

            assertEquals(
                expected = null to 2,
                actual = placesViewModel.state.value.failureDialog to favoriteRepository.favorites.value.size
            )
        }

    @Test
    fun `submitting before the places of the last letters are shown saves a place for the whole query`() =
        testScope.runTest {
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()
            placesViewModel.onEvent(event = PlacesEvent.OnSearchQueryChanged(query = "K"))
            advanceUntilIdle()

            placesViewModel.onEvent(event = PlacesEvent.OnSearchQueryChanged(query = "Ka"))
            placesViewModel.onEvent(event = PlacesEvent.OnSearchSubmitted(query = "Ka"))
            advanceUntilIdle()

            assertEquals(expected = "Kalmar", actual = favoriteRepository.favorites.value.last().locality)
        }

    @Test
    fun `saved places that cannot be read say so`() =
        testScope.runTest {
            favoriteRepository.failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)

            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                expected = storageFailureDialog to emptyList<Favorite>(),
                actual = placesViewModel.state.value.failureDialog to placesViewModel.state.value.favoriteChoices.map { favoriteChoice: FavoriteChoice -> favoriteChoice.favorite }
            )
        }

    @Test
    fun `a place that cannot be chosen says why`() =
        testScope.runTest {
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()
            locationRepository.writeFailure = Failure(errorCode = ErrorCode.STORAGE_FAILED)

            placesViewModel.onEvent(event = PlacesEvent.OnPlaceChosen(favorite = visby))
            advanceUntilIdle()

            assertEquals(expected = storageFailureDialog, actual = placesViewModel.state.value.failureDialog)
            assertEquals(expected = listOf("Umeå" to true, "Visby" to false), actual = placesViewModel.shownPlaces())
        }

    @Test
    fun `a place that cannot be removed says why`() =
        testScope.runTest {
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()
            favoriteRepository.removeFailure = Failure(errorCode = ErrorCode.STORAGE_FAILED)

            placesViewModel.onEvent(event = PlacesEvent.OnPlaceRemoved(favoriteId = visby.favoriteId))
            advanceUntilIdle()

            assertEquals(expected = storageFailureDialog, actual = placesViewModel.state.value.failureDialog)
            assertEquals(expected = listOf("Umeå" to true, "Visby" to false), actual = placesViewModel.shownPlaces())
        }

    @Test
    fun `leaving the position when it cannot be stored says why`() =
        testScope.runTest {
            locationRepository.setAutomatic(isAutomatic = true)
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()
            locationRepository.writeFailure = Failure(errorCode = ErrorCode.STORAGE_FAILED)

            placesViewModel.onEvent(event = PlacesEvent.OnFollowPositionToggled(isFollowingPosition = false))
            advanceUntilIdle()

            assertEquals(
                expected = true to storageFailureDialog,
                actual = placesViewModel.state.value.isFollowingPosition to placesViewModel.state.value.failureDialog
            )
        }

    @Test
    fun `a search that fails says why`() =
        testScope.runTest {
            val placesViewModel: PlacesViewModel = viewModel()
            advanceUntilIdle()
            placeRepository.failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)

            placesViewModel.onEvent(event = PlacesEvent.OnSearchQueryChanged(query = "Ki"))
            advanceUntilIdle()

            assertEquals(
                expected = storageFailureDialog to persistentListOf(),
                actual = placesViewModel.state.value.failureDialog to placesViewModel.state.value.suggestions
            )
        }
}