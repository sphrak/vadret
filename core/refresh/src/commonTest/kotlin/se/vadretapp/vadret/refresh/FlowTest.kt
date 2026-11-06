package se.vadretapp.vadret.refresh

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
import kotlin.test.Test
import kotlin.test.assertEquals

private const val NOT_REFRESHED = "not refreshed"

private val nothingSaved: List<String> = emptyList()
private const val NETWORK_UNAVAILABLE = "network unavailable"
private const val UNREADABLE = "unreadable"

class FlowTest {

    private val saved: MutableStateFlow<List<String>> = MutableStateFlow(value = nothingSaved)

    private fun savedFlow(): Flow<Either<String, List<String>>> = saved.map { names: List<String> -> names.asRight() }

    private fun refreshing(
        isRefreshFirst: Boolean = false,
        refresh: suspend () -> Either<String, List<String>>
    ): Flow<Either<String, List<String>>> =
        savedFlow().whileRefreshing(
            refresh = refresh,
            isNothingSaved = { names: List<String> -> names.isEmpty() },
            whenRefreshFails = { names: List<String> -> names + NOT_REFRESHED },
            isRefreshFirst = isRefreshFirst
        )

    @Test
    fun `saved data shows at once and the refreshed data once it is saved`() =
        runTest {
            saved.value = listOf("hassel")
            val refreshGate = CompletableDeferred<Unit>()
            val seen = mutableListOf<Either<String, List<String>>>()

            val collecting = launch {
                refreshing {
                    refreshGate.await()
                    saved.value = listOf("hassel", "björk")
                    saved.value.asRight()
                }.take(count = 2).toList(destination = seen)
            }
            testScheduler.runCurrent()
            val beforeRefresh: List<Either<String, List<String>>> = seen.toList()
            refreshGate.complete(value = Unit)
            collecting.join()

            assertEquals<Pair<List<Either<String, List<String>>>, List<Either<String, List<String>>>>>(
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
                expected = listOf(NETWORK_UNAVAILABLE.asLeft()),
                actual = refreshing { NETWORK_UNAVAILABLE.asLeft() }.take(count = 1).toList()
            )
        }

    @Test
    fun `with saved data a refresh that fails shows the saved data as not refreshed`() =
        runTest {
            saved.value = listOf("hassel")
            val refreshGate = CompletableDeferred<Unit>()
            val seen = mutableListOf<Either<String, List<String>>>()

            val collecting = launch {
                refreshing {
                    refreshGate.await()
                    NETWORK_UNAVAILABLE.asLeft()
                }.toList(destination = seen)
            }
            testScheduler.advanceUntilIdle()
            refreshGate.complete(value = Unit)
            testScheduler.advanceUntilIdle()
            collecting.cancel()

            assertEquals<List<Either<String, List<String>>>>(
                expected = listOf(listOf("hassel").asRight(), listOf("hassel", NOT_REFRESHED).asRight()),
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

    @Test
    fun `refreshing first shows the refreshed data and not the saved data before it`() =
        runTest {
            saved.value = listOf("hassel")

            assertEquals(
                expected = listOf(listOf("hassel", "björk").asRight()),
                actual = refreshing(isRefreshFirst = true) {
                    saved.value = listOf("hassel", "björk")
                    saved.value.asRight()
                }.take(count = 1).toList()
            )
        }

    @Test
    fun `refreshing first that fails shows only the saved data as not refreshed`() =
        runTest {
            saved.value = listOf("hassel")

            assertEquals(
                expected = listOf(listOf("hassel", NOT_REFRESHED).asRight()),
                actual = refreshing(isRefreshFirst = true) { NETWORK_UNAVAILABLE.asLeft() }.take(count = 1).toList()
            )
        }

    @Test
    fun `refreshing first that fails without saved data is the failure`() =
        runTest {
            assertEquals(
                expected = listOf(NETWORK_UNAVAILABLE.asLeft()),
                actual = refreshing(isRefreshFirst = true) { NETWORK_UNAVAILABLE.asLeft() }.take(count = 1).toList()
            )
        }

    @Test
    fun `saved data that cannot be read is told as it is`() =
        runTest {
            val unreadable: Flow<Either<String, List<String>>> = MutableStateFlow<Either<String, List<String>>>(value = UNREADABLE.asLeft())

            assertEquals(
                expected = listOf<Either<String, List<String>>>(UNREADABLE.asLeft()),
                actual = unreadable
                    .whileRefreshing(
                        refresh = { listOf("björk").asRight() },
                        isNothingSaved = { names: List<String> -> names.isEmpty() },
                        whenRefreshFails = { names: List<String> -> names },
                        isRefreshFirst = false
                    ).take(count = 1)
                    .toList()
            )
        }
}