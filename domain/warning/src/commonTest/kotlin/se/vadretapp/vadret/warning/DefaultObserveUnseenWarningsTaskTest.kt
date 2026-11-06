package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.GeoPolygon
import se.vadretapp.vadret.aggregatedfeed.model.SeenWarning
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.warning.model.WarningPlace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val publishedAt: Instant = Instant.parse(input = "2026-10-28T08:00:00Z")

private val funasdalen = WarningPlace(
    latitude = 62.546,
    longitude = 12.542,
    county = "Jämtland",
    municipality = "Härjedalen"
)

private val aroundFunasdalen = GeoPolygon(
    outline = square(
        south = 62.4,
        west = 12.3,
        north = 62.7,
        east = 12.9
    ),
    holes = emptyList()
)

private val aroundGotland = GeoPolygon(
    outline = square(
        south = 56.9,
        west = 18.0,
        north = 57.9,
        east = 19.3
    ),
    holes = emptyList()
)

class DefaultObserveUnseenWarningsTaskTest {

    private val newest = aggregatedFeed(
        identifier = "smhi-3-1",
        publishedAt = publishedAt + 2.minutes,
        polygons = listOf(aroundFunasdalen)
    )
    private val elsewhere = aggregatedFeed(
        identifier = "smhi-2-1",
        publishedAt = publishedAt + 1.minutes,
        polygons = listOf(aroundGotland)
    )
    private val oldest = aggregatedFeed(
        identifier = "smhi-1-1",
        publishedAt = publishedAt,
        polygons = listOf(aroundFunasdalen)
    )
    private val feedRepository = FakeFeedRepository(result = listOf(newest, elsewhere, oldest).asRight())
    private val observeUnseenWarningsTask: ObserveUnseenWarningsTask = DefaultObserveUnseenWarningsTask(feedRepository = feedRepository)

    private fun seen(
        warningIdentifier: String,
        seenAt: Instant
    ): SeenWarning =
        SeenWarning(
            warningIdentifier = warningIdentifier,
            seenAt = seenAt
        )

    @Test
    fun `only warnings that affect the place are kept newest first`() =
        runTest {
            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = listOf(newest, oldest).asRight(),
                actual = observeUnseenWarningsTask(
                    warningPlace = funasdalen,
                    isRefresh = false
                ).first()
            )
        }

    @Test
    fun `a warning seen after it was published is left out but one updated since is kept`() =
        runTest {
            feedRepository.seenWarnings.value = listOf(
                seen(
                    warningIdentifier = newest.identifier,
                    seenAt = newest.publishedAt
                ),
                seen(
                    warningIdentifier = oldest.identifier,
                    seenAt = oldest.publishedAt - 1.minutes
                )
            ).asRight()

            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = listOf(oldest).asRight(),
                actual = observeUnseenWarningsTask(
                    warningPlace = funasdalen,
                    isRefresh = false
                ).first()
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `seeing a warning emits the rest without asking the feed again`() =
        runTest {
            val emitted = mutableListOf<Either<Failure, List<AggregatedFeed>>>()
            val job = launch {
                observeUnseenWarningsTask(
                    warningPlace = funasdalen,
                    isRefresh = true
                ).toList(destination = emitted)
            }
            advanceUntilIdle()

            feedRepository.seenWarnings.value = listOf(
                seen(
                    warningIdentifier = elsewhere.identifier,
                    seenAt = publishedAt + 5.minutes
                )
            ).asRight()
            advanceUntilIdle()
            feedRepository.seenWarnings.value = listOf(
                seen(
                    warningIdentifier = newest.identifier,
                    seenAt = publishedAt + 5.minutes
                )
            ).asRight()
            advanceUntilIdle()
            job.cancel()

            assertEquals<List<Either<Failure, List<AggregatedFeed>>>>(
                expected = listOf(
                    listOf(newest, oldest).asRight(),
                    listOf(oldest).asRight()
                ),
                actual = emitted
            )
            assertEquals(expected = 1, actual = feedRepository.refreshCount)
        }

    @Test
    fun `a failing feed is emitted once`() =
        runTest {
            feedRepository.result = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()

            assertEquals<List<Either<Failure, List<AggregatedFeed>>>>(
                expected = listOf(Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()),
                actual = observeUnseenWarningsTask(
                    warningPlace = funasdalen,
                    isRefresh = false
                ).take(count = 1).toList()
            )
        }

    @Test
    fun `failing seen storage is emitted`() =
        runTest {
            feedRepository.seenWarnings.value = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft()

            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft(),
                actual = observeUnseenWarningsTask(
                    warningPlace = funasdalen,
                    isRefresh = false
                ).first()
            )
        }
}