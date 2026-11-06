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
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegion
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegionKind
import se.vadretapp.vadret.aggregatedfeed.model.GeoPolygon
import se.vadretapp.vadret.aggregatedfeed.model.SeenWarning
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.warning.model.FollowedPlaceWarnings
import se.vadretapp.vadret.warning.model.PlaceWarning
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

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

private val aroundSkane = GeoPolygon(
    outline = square(
        south = 55.3,
        west = 12.4,
        north = 56.5,
        east = 14.6
    ),
    holes = emptyList()
)

class DefaultObserveFollowedPlaceWarningsTaskTest {

    private val inFunasdalen = aggregatedFeed(
        identifier = "smhi-1-1",
        polygons = listOf(aroundFunasdalen)
    )
    private val onGotland = aggregatedFeed(
        identifier = "smhi-2-1",
        polygons = listOf(aroundGotland)
    )
    private val inSkane = aggregatedFeed(
        identifier = "smhi-3-1",
        polygons = listOf(aroundSkane)
    )
    private val overBoth = aggregatedFeed(
        identifier = "krisinformation-1",
        polygons = listOf(aroundFunasdalen),
        regions = listOf(
            FeedRegion(
                kind = FeedRegionKind.COUNTY,
                name = "Gotlands län"
            )
        )
    )
    private val feedRepository = FakeFeedRepository(result = listOf(inFunasdalen, onGotland, inSkane, overBoth).asRight())
    private val observeFollowedPlacesTask = FakeObserveFollowedPlacesTask()
    private val observeFollowedPlaceWarningsTask: ObserveFollowedPlaceWarningsTask = DefaultObserveFollowedPlaceWarningsTask(
        observeFollowedPlacesTask = observeFollowedPlacesTask,
        feedRepository = feedRepository
    )

    @Test
    fun `every warning that reaches a followed place is there with the places it reaches`() =
        runTest {
            observeFollowedPlacesTask.followedPlaces.value = listOf(followedFunasdalen, followedVisby).asRight()

            assertEquals<Either<Failure, FollowedPlaceWarnings>>(
                expected = FollowedPlaceWarnings(
                    followedPlaces = listOf(followedFunasdalen, followedVisby),
                    placeWarnings = listOf(
                        PlaceWarning(
                            aggregatedFeed = inFunasdalen,
                            followedPlaces = listOf(followedFunasdalen)
                        ),
                        PlaceWarning(
                            aggregatedFeed = onGotland,
                            followedPlaces = listOf(followedVisby)
                        ),
                        PlaceWarning(
                            aggregatedFeed = overBoth,
                            followedPlaces = listOf(followedFunasdalen)
                        )
                    )
                ).asRight(),
                actual = observeFollowedPlaceWarningsTask(isRefresh = false).first()
            )
        }

    @Test
    fun `a warning already seen stays among the warnings of the followed places`() =
        runTest {
            feedRepository.seenWarnings.value = listOf(
                SeenWarning(
                    warningIdentifier = inFunasdalen.identifier,
                    seenAt = Instant.parse(input = "2026-10-29T08:00:00Z")
                )
            ).asRight()

            val placeWarnings: List<PlaceWarning> = (observeFollowedPlaceWarningsTask(isRefresh = false).first() as Either.Right).b.placeWarnings

            assertEquals(
                expected = listOf(inFunasdalen, overBoth),
                actual = placeWarnings.map { placeWarning: PlaceWarning -> placeWarning.aggregatedFeed }
            )
        }

    @Test
    fun `followed places that cannot be read are the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            observeFollowedPlacesTask.followedPlaces.value = failure.asLeft()

            assertEquals<Either<Failure, FollowedPlaceWarnings>>(expected = failure.asLeft(), actual = observeFollowedPlaceWarningsTask(isRefresh = false).first())
        }

    @Test
    fun `warnings that cannot be loaded are the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.NETWORK_FAILURE)
            feedRepository.result = failure.asLeft()

            assertEquals<Either<Failure, FollowedPlaceWarnings>>(expected = failure.asLeft(), actual = observeFollowedPlaceWarningsTask(isRefresh = false).first())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `following another place gives its warnings too`() =
        runTest {
            val emissions: MutableList<Either<Failure, FollowedPlaceWarnings>> = mutableListOf()
            val collection = launch { observeFollowedPlaceWarningsTask(isRefresh = false).toList(destination = emissions) }
            advanceUntilIdle()

            observeFollowedPlacesTask.followedPlaces.value = listOf(followedFunasdalen, followedVisby).asRight()
            advanceUntilIdle()
            collection.cancel()

            assertEquals(
                expected = listOf(
                    listOf("smhi-1-1", "krisinformation-1"),
                    listOf("smhi-1-1", "smhi-2-1", "krisinformation-1")
                ),
                actual = emissions.map { followedPlaceWarnings: Either<Failure, FollowedPlaceWarnings> ->
                    (followedPlaceWarnings as Either.Right).b.placeWarnings.map { placeWarning: PlaceWarning -> placeWarning.aggregatedFeed.identifier }
                }
            )
        }

    @Test
    fun `a refresh asks for fresh warnings`() =
        runTest {
            observeFollowedPlaceWarningsTask(isRefresh = true).first()

            assertEquals(expected = 1, actual = feedRepository.refreshCount)
        }
}