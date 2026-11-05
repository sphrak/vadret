package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.followedplace.extension.toStoredLocation
import se.vadretapp.vadret.followedplace.model.FavoriteChoice
import se.vadretapp.vadret.followedplace.model.LocationChoices
import se.vadretapp.vadret.location.model.LocationPreference
import kotlin.test.Test
import kotlin.test.assertEquals

private val bergaDanderyd = Favorite(
    favoriteId = "berga-danderyd",
    locality = "Berga",
    municipality = "Danderyd",
    county = "Stockholms län",
    latitude = 59.4,
    longitude = 18.05
)

private val bergaLinkoping = bergaDanderyd.copy(
    favoriteId = "berga-linkoping",
    municipality = "Linköping",
    county = "Östergötlands län"
)

class DefaultObserveLocationChoicesTaskTest {

    private val locationRepository = FakeLocationRepository(isAutomatic = false)
    private val favoriteRepository = FakeFavoriteRepository(initialFavorites = emptyList())
    private val observeLocationChoicesTask: ObserveLocationChoicesTask = DefaultObserveLocationChoicesTask(
        favoriteRepository = favoriteRepository,
        locationRepository = locationRepository
    )

    private fun prefer(
        isAutomatic: Boolean,
        favorite: Favorite
    ) {
        favoriteRepository.favorites.value = listOf(bergaDanderyd, bergaLinkoping)
        locationRepository.locationPreference.value = LocationPreference(
            isAutomatic = isAutomatic,
            storedLocation = favorite.toStoredLocation()
        )
    }

    @Test
    fun `automatic selects no favorite`() =
        runTest {
            prefer(
                isAutomatic = true,
                favorite = bergaDanderyd
            )

            assertEquals<Either<Failure, LocationChoices>>(
                expected = LocationChoices(
                    isAutomatic = true,
                    chosenPlaceName = null,
                    favorites = persistentListOf(
                        FavoriteChoice(favorite = bergaDanderyd, isSelected = false, isFollowed = true),
                        FavoriteChoice(favorite = bergaLinkoping, isSelected = false, isFollowed = true)
                    )
                ).asRight(),
                actual = observeLocationChoicesTask().first()
            )
        }

    @Test
    fun `the stored place selects only the favorite in the same municipality`() =
        runTest {
            prefer(
                isAutomatic = false,
                favorite = bergaLinkoping
            )

            assertEquals<Either<Failure, LocationChoices>>(
                expected = LocationChoices(
                    isAutomatic = false,
                    chosenPlaceName = "Berga",
                    favorites = persistentListOf(
                        FavoriteChoice(favorite = bergaDanderyd, isSelected = false, isFollowed = true),
                        FavoriteChoice(favorite = bergaLinkoping, isSelected = true, isFollowed = true)
                    )
                ).asRight(),
                actual = observeLocationChoicesTask().first()
            )
        }

    @Test
    fun `a stored place that is no favorite selects nothing`() =
        runTest {
            prefer(
                isAutomatic = false,
                favorite = kirunaFavorite
            )

            assertEquals(
                expected = listOf(false, false),
                actual = observeLocationChoicesTask()
                    .first()
                    .successOrEmpty()
                    .map { favoriteChoice: FavoriteChoice -> favoriteChoice.isSelected }
            )
        }

    @Test
    fun `saved places past the limit are listed but not followed`() =
        runTest {
            val manyFavorites: List<Favorite> = (1..6).map { number: Int ->
                bergaDanderyd.copy(
                    favoriteId = "favorite-$number",
                    locality = "Plats $number"
                )
            }
            favoriteRepository.favorites.value = manyFavorites
            locationRepository.locationPreference.value = LocationPreference(
                isAutomatic = false,
                storedLocation = manyFavorites.last().toStoredLocation()
            )

            assertEquals(
                expected = listOf(true, true, true, true, false, true),
                actual = observeLocationChoicesTask()
                    .first()
                    .successOrEmpty()
                    .map { favoriteChoice: FavoriteChoice -> favoriteChoice.isFollowed }
            )
        }

    @Test
    fun `the position leaves room for four saved places`() =
        runTest {
            favoriteRepository.favorites.value = (1..5).map { number: Int ->
                bergaDanderyd.copy(
                    favoriteId = "favorite-$number",
                    locality = "Plats $number"
                )
            }

            assertEquals(
                expected = listOf(true, true, true, true, false),
                actual = observeLocationChoicesTask()
                    .first()
                    .successOrEmpty()
                    .map { favoriteChoice: FavoriteChoice -> favoriteChoice.isFollowed }
            )
        }

    @Test
    fun `a favorite failure fails the choices`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            favoriteRepository.failure = failure

            assertEquals<Either<Failure, LocationChoices>>(
                expected = failure.asLeft(),
                actual = observeLocationChoicesTask().first()
            )
        }

    private fun Either<Failure, LocationChoices>.successOrEmpty(): List<FavoriteChoice> =
        when (this) {
            is Either.Left -> emptyList()
            is Either.Right -> b.favorites
        }
}