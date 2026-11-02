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
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegion
import se.vadretapp.vadret.aggregatedfeed.model.FeedRegionKind
import se.vadretapp.vadret.aggregatedfeed.model.GeoPolygon
import se.vadretapp.vadret.aggregatedfeed.model.SeenWarning
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val funasdalen = StoredLocation(
    locality = "Funäsdalen",
    municipality = "Härjedalen",
    county = "Jämtland",
    latitude = 62.546,
    longitude = 12.542
)

private val visby = StoredLocation(
    locality = "Visby",
    municipality = "Gotland",
    county = "Gotland",
    latitude = 57.634,
    longitude = 18.294
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

private val jamtland = FeedRegion(
    kind = FeedRegionKind.COUNTY,
    name = "Jämtlands län"
)

class DefaultObserveChosenPlaceWarningsTaskTest {

    private val inFunasdalen = aggregatedFeed(
        identifier = "smhi-1-1",
        polygons = listOf(aroundFunasdalen)
    )
    private val onGotland = aggregatedFeed(
        identifier = "smhi-2-1",
        polygons = listOf(aroundGotland)
    )
    private val inJamtland = aggregatedFeed(
        identifier = "krisinformation-1",
        regions = listOf(jamtland)
    )
    private val feedRepository = FakeFeedRepository(result = listOf(inFunasdalen, onGotland, inJamtland).asRight())
    private val locationRepository = FakeLocationRepository(storedLocation = funasdalen)
    private val nominatimRepository = FakeNominatimRepository()
    private val observeChosenPlaceWarningsTask: ObserveChosenPlaceWarningsTask = DefaultObserveChosenPlaceWarningsTask(
        feedRepository = feedRepository,
        locationRepository = locationRepository,
        nominatimRepository = nominatimRepository
    )

    private fun chooseAutomatic() {
        locationRepository.locationPreference.value = LocationPreference(
            isAutomatic = true,
            storedLocation = visby
        ).asRight()
    }

    @Test
    fun `a chosen place gets the warnings that reach it or name its region`() =
        runTest {
            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = listOf(inFunasdalen, inJamtland).asRight(),
                actual = observeChosenPlaceWarningsTask(isRefresh = false).first()
            )
        }

    @Test
    fun `warnings already seen stay among the warnings of the place`() =
        runTest {
            feedRepository.seenWarnings.value = listOf(
                SeenWarning(
                    warningIdentifier = inFunasdalen.identifier,
                    seenAt = Instant.parse(input = "2026-10-29T08:00:00Z")
                )
            ).asRight()

            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = listOf(inFunasdalen, inJamtland).asRight(),
                actual = observeChosenPlaceWarningsTask(isRefresh = false).first()
            )
        }

    @Test
    fun `the device location is named by Nominatim so warnings for its region reach it`() =
        runTest {
            chooseAutomatic()

            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = listOf(inFunasdalen, inJamtland).asRight(),
                actual = observeChosenPlaceWarningsTask(isRefresh = false).first()
            )
        }

    @Test
    fun `a device location Nominatim cannot name only gets the warnings that reach its coordinates`() =
        runTest {
            chooseAutomatic()
            nominatimRepository.locality = Failure(errorCode = ErrorCode.NETWORK_UNAVAILABLE).asLeft()

            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = listOf(inFunasdalen).asRight(),
                actual = observeChosenPlaceWarningsTask(isRefresh = false).first()
            )
        }

    @Test
    fun `a device location that cannot be found is the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.LOCATION_NOT_AVAILABLE)
            chooseAutomatic()
            locationRepository.deviceLocation = failure.asLeft()

            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = failure.asLeft(),
                actual = observeChosenPlaceWarningsTask(isRefresh = false).first()
            )
        }

    @Test
    fun `warnings that cannot be loaded are the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.NETWORK_FAILURE)
            feedRepository.result = failure.asLeft()

            assertEquals<Either<Failure, List<AggregatedFeed>>>(
                expected = failure.asLeft(),
                actual = observeChosenPlaceWarningsTask(isRefresh = false).first()
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `choosing another place gives the warnings of that place`() =
        runTest {
            val emissions: MutableList<Either<Failure, List<AggregatedFeed>>> = mutableListOf()
            val collection = launch { observeChosenPlaceWarningsTask(isRefresh = false).toList(destination = emissions) }
            advanceUntilIdle()

            locationRepository.locationPreference.value = LocationPreference(
                isAutomatic = false,
                storedLocation = visby
            ).asRight()
            advanceUntilIdle()
            collection.cancel()

            assertEquals<List<Either<Failure, List<AggregatedFeed>>>>(
                expected = listOf(
                    listOf(inFunasdalen, inJamtland).asRight(),
                    listOf(onGotland).asRight()
                ),
                actual = emissions
            )
        }

    @Test
    fun `a refresh asks for fresh warnings`() =
        runTest {
            observeChosenPlaceWarningsTask(isRefresh = true).first()

            assertEquals(expected = listOf(true), actual = feedRepository.refreshes)
        }
}