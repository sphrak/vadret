package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultSetLocationModeTaskTest {

    private val locationRepository = FakeLocationRepository(isAutomatic = false)
    private val favoriteRepository = FakeFavoriteRepository(initialFavorites = listOf(umea, visby, cabin, kirunaFavorite))
    private val setLocationModeTask: SetLocationModeTask = DefaultSetLocationModeTask(
        locationRepository = locationRepository,
        favoriteRepository = favoriteRepository
    )

    @Test
    fun `the position is followed next to four saved places`() =
        runTest {
            assertEquals<Either<Failure, Unit>>(
                expected = Unit.asRight(),
                actual = setLocationModeTask(isAutomatic = true)
            )
            assertEquals(expected = listOf("automatic=true"), actual = locationRepository.writes)
        }

    @Test
    fun `the position is refused next to five saved places`() =
        runTest {
            favoriteRepository.favorites.value = favoriteRepository.favorites.value + umea.copy(favoriteId = "favorite-north", locality = "Norr")

            assertEquals<Either<Failure, Unit>>(
                expected = Failure(errorCode = ErrorCode.FOLLOWED_PLACE_LIMIT).asLeft(),
                actual = setLocationModeTask(isAutomatic = true)
            )
            assertEquals(expected = emptyList(), actual = locationRepository.writes)
        }

    @Test
    fun `a position already followed stays followed next to five saved places`() =
        runTest {
            locationRepository.locationPreference.value = locationRepository.locationPreference.value.copy(isAutomatic = true)
            favoriteRepository.favorites.value = favoriteRepository.favorites.value + umea.copy(favoriteId = "favorite-north", locality = "Norr")

            assertEquals<Either<Failure, Unit>>(
                expected = Unit.asRight(),
                actual = setLocationModeTask(isAutomatic = true)
            )
            assertEquals(expected = emptyList(), actual = locationRepository.writes)
        }

    @Test
    fun `a location mode that cannot be read leaves the mode as it is`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            locationRepository.failure = failure

            assertEquals<Either<Failure, Unit>>(
                expected = failure.asLeft(),
                actual = setLocationModeTask(isAutomatic = true)
            )
            assertEquals(expected = emptyList(), actual = locationRepository.writes)
        }

    @Test
    fun `leaving the position is always allowed`() =
        runTest {
            favoriteRepository.favorites.value = favoriteRepository.favorites.value + umea.copy(favoriteId = "favorite-north", locality = "Norr")

            assertEquals<Either<Failure, Unit>>(
                expected = Unit.asRight(),
                actual = setLocationModeTask(isAutomatic = false)
            )
        }

    @Test
    fun `saved places that cannot be read leave the mode as it is`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            favoriteRepository.failure = failure

            assertEquals<Either<Failure, Unit>>(
                expected = failure.asLeft(),
                actual = setLocationModeTask(isAutomatic = true)
            )
            assertEquals(expected = emptyList(), actual = locationRepository.writes)
        }
}