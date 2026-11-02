package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultSetPlaceFollowedTaskTest {

    private val locationRepository = FakeLocationRepository()
    private val setPlaceFollowedTask: SetPlaceFollowedTask = DefaultSetPlaceFollowedTask(locationRepository = locationRepository)

    @Test
    fun `a favorite is followed and unfollowed`() =
        runTest {
            setPlaceFollowedTask(
                favoriteId = "favorite-visby",
                isFollowed = true
            )
            val followed: Set<String> = locationRepository.locationPreference.value.followedFavoriteIds

            setPlaceFollowedTask(
                favoriteId = "favorite-visby",
                isFollowed = false
            )

            assertEquals(expected = setOf("favorite-visby"), actual = followed)
            assertEquals(expected = emptySet(), actual = locationRepository.locationPreference.value.followedFavoriteIds)
        }

    @Test
    fun `four favorites besides the chosen place can be followed and a fifth says why it is not`() =
        runTest {
            val followed: List<Either<Failure, Unit>> = (1..5).map { number: Int ->
                setPlaceFollowedTask(
                    favoriteId = "favorite-$number",
                    isFollowed = true
                )
            }

            assertEquals(
                expected = List(size = 4) { Unit.asRight() } + Failure(errorCode = ErrorCode.FOLLOWED_PLACE_LIMIT).asLeft(),
                actual = followed
            )
            assertEquals(expected = 4, actual = locationRepository.locationPreference.value.followedFavoriteIds.size)
        }
}