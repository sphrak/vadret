package se.vadretapp.vadret.warning

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.aggregatedfeed.model.AggregatedFeed
import se.vadretapp.vadret.aggregatedfeed.model.GeoPolygon
import se.vadretapp.vadret.aggregatedfeed.model.SeenWarning
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val publishedAt: Instant = Instant.parse(input = "2026-10-28T08:00:00Z")

class DefaultObserveUnseenFollowedPlaceWarningsTaskTest {

    private val inFunasdalen = aggregatedFeed(
        identifier = "smhi-1-1",
        publishedAt = publishedAt,
        polygons = listOf(
            GeoPolygon(
                outline = square(
                    south = 62.4,
                    west = 12.3,
                    north = 62.7,
                    east = 12.9
                ),
                holes = emptyList()
            )
        )
    )
    private val onGotland = aggregatedFeed(
        identifier = "smhi-2-1",
        publishedAt = publishedAt,
        polygons = listOf(
            GeoPolygon(
                outline = square(
                    south = 56.9,
                    west = 18.0,
                    north = 57.9,
                    east = 19.3
                ),
                holes = emptyList()
            )
        )
    )
    private val feedRepository = FakeFeedRepository(result = listOf(inFunasdalen, onGotland).asRight())
    private val observeFollowedPlacesTask = FakeObserveFollowedPlacesTask()
    private val observeUnseenFollowedPlaceWarningsTask: ObserveUnseenFollowedPlaceWarningsTask = DefaultObserveUnseenFollowedPlaceWarningsTask(
        observeFollowedPlacesTask = observeFollowedPlacesTask,
        feedRepository = feedRepository
    )

    @Test
    fun `unseen warnings of every followed place are kept`() =
        runTest {
            observeFollowedPlacesTask.followedPlaces.value = listOf(followedFunasdalen, followedVisby).asRight()

            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = listOf(inFunasdalen, onGotland).asRight(),
                actual = observeUnseenFollowedPlaceWarningsTask(isRefresh = false).first()
            )
        }

    @Test
    fun `a warning seen since it was published is left out`() =
        runTest {
            observeFollowedPlacesTask.followedPlaces.value = listOf(followedFunasdalen, followedVisby).asRight()
            feedRepository.seenWarnings.value = listOf(
                SeenWarning(
                    warningIdentifier = onGotland.identifier,
                    seenAt = publishedAt
                )
            ).asRight()

            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = listOf(inFunasdalen).asRight(),
                actual = observeUnseenFollowedPlaceWarningsTask(isRefresh = false).first()
            )
        }

    @Test
    fun `followed places that cannot be read are the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            observeFollowedPlacesTask.followedPlaces.value = failure.asLeft()

            assertEquals<Either<Failure, List<AggregatedFeed>>>(expected = failure.asLeft(), actual = observeUnseenFollowedPlaceWarningsTask(isRefresh = false).first())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `following another place adds its unseen warnings`() =
        runTest {
            val emissions: MutableList<Either<Failure, List<AggregatedFeed>>> = mutableListOf()
            val collection = launch { observeUnseenFollowedPlaceWarningsTask(isRefresh = false).toList(destination = emissions) }
            advanceUntilIdle()

            observeFollowedPlacesTask.followedPlaces.value = listOf(followedFunasdalen, followedVisby).asRight()
            advanceUntilIdle()
            collection.cancel()

            assertEquals<List<Either<Failure, List<AggregatedFeed>>>>(
                expected = listOf(
                    listOf(inFunasdalen).asRight(),
                    listOf(inFunasdalen, onGotland).asRight()
                ),
                actual = emissions
            )
        }
}