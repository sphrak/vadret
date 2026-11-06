package se.vadretapp.vadret.common.extension

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import kotlin.test.Test
import kotlin.test.assertEquals

private val nothingSaved: List<String> = emptyList()
private val networkUnavailable = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE)

class FlowTest {

    private val saved: MutableStateFlow<List<String>> = MutableStateFlow(value = nothingSaved)

    private fun savedFlow(): Flow<Either<Failure, List<String>>> = saved.map { names: List<String> -> names.asRight() }

    private fun refreshing(refresh: suspend () -> Either<Failure, List<String>>): Flow<Either<Failure, List<String>>> =
        savedFlow().whileRefreshing(
            refresh = refresh,
            isNothingSaved = { names: List<String> -> names.isEmpty() }
        )

    @Test
    fun `saved data shows at once and the refreshed data once it is saved`() =
        runTest {
            saved.value = listOf("hassel")
            val refreshGate = CompletableDeferred<Unit>()
            val seen = mutableListOf<Either<Failure, List<String>>>()

            val collecting = launch {
                refreshing {
                    refreshGate.await()
                    saved.value = listOf("hassel", "björk")
                    saved.value.asRight()
                }.take(count = 2).toList(destination = seen)
            }
            testScheduler.runCurrent()
            val beforeRefresh: List<Either<Failure, List<String>>> = seen.toList()
            refreshGate.complete(value = Unit)
            collecting.join()

            assertEquals<Pair<List<Either<Failure, List<String>>>, List<Either<Failure, List<String>>>>>(
                expected = listOf(listOf("hassel").asRight()) to listOf(listOf("hassel").asRight(), listOf("hassel", "björk").asRight()),
                actual = beforeRefresh to seen
            )
        }

    @Test
    fun `without saved data the refreshed data is the first shown`() =
        runTest {
            assertEquals(
                expected = listOf(listOf("björk").asRight()),
                actual = refreshing {
                    saved.value = listOf("björk")
                    saved.value.asRight()
                }.take(count = 1).toList()
            )
        }

    @Test
    fun `without saved data a refresh that fails is the failure`() =
        runTest {
            assertEquals(
                expected = listOf(networkUnavailable.asLeft()),
                actual = refreshing { networkUnavailable.asLeft() }.take(count = 1).toList()
            )
        }

    @Test
    fun `with saved data a refresh that fails leaves the saved data`() =
        runTest {
            saved.value = listOf("hassel")
            val seen = mutableListOf<Either<Failure, List<String>>>()

            val collecting = launch {
                refreshing { networkUnavailable.asLeft() }.toList(destination = seen)
            }
            testScheduler.advanceUntilIdle()
            collecting.cancel()

            assertEquals<List<Either<Failure, List<String>>>>(
                expected = listOf(listOf("hassel").asRight()),
                actual = seen
            )
        }

    @Test
    fun `a refresh that finds nothing shows that nothing is there`() =
        runTest {
            assertEquals(
                expected = listOf(nothingSaved.asRight()),
                actual = refreshing { nothingSaved.asRight() }.take(count = 1).toList()
            )
        }
}