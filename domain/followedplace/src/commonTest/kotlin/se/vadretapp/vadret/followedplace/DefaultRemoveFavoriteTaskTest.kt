package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultRemoveFavoriteTaskTest {

    private val favoriteRepository = FakeFavoriteRepository()
    private val removeFavoriteTask: RemoveFavoriteTask = DefaultRemoveFavoriteTask(favoriteRepository = favoriteRepository)

    @Test
    fun `a removed favorite leaves the others`() =
        runTest {
            assertEquals<Either<Failure, Unit>>(
                expected = Unit.asRight(),
                actual = removeFavoriteTask(favoriteId = visby.favoriteId)
            )
            assertEquals(expected = listOf(umea, cabin), actual = favoriteRepository.favorites.value)
        }

    @Test
    fun `a favorite that cannot be removed is the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            favoriteRepository.failure = failure

            assertEquals<Either<Failure, Unit>>(
                expected = failure.asLeft(),
                actual = removeFavoriteTask(favoriteId = visby.favoriteId)
            )
        }
}