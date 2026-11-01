package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultGetWarningsTaskTest {

    private val feedRepository = FakeFeedRepository()
    private val getWarningsTask: GetWarningsTask = DefaultGetWarningsTask(feedRepository = feedRepository)

    @Test
    fun `every warning is returned unfiltered`() =
        runTest {
            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = feedRepository.result,
                actual = getWarningsTask(isRefresh = false)
            )
        }

    @Test
    fun `a refresh is passed on to the feed`() =
        runTest {
            getWarningsTask(isRefresh = true)
            getWarningsTask(isRefresh = false)

            assertEquals(expected = listOf(true, false), actual = feedRepository.refreshes)
        }

    @Test
    fun `a failing feed fails the task`() =
        runTest {
            feedRepository.result = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()

            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft(),
                actual = getWarningsTask(isRefresh = false)
            )
        }
}