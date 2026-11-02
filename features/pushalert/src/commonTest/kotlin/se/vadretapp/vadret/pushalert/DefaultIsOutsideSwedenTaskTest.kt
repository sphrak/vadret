package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.pushalert.usecase.DefaultGetCurrentGeohashTask
import se.vadretapp.vadret.pushalert.usecase.DefaultIsOutsideSwedenTask
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultIsOutsideSwedenTaskTest {
    @Test
    fun aPhoneInSwedenOrFollowingASavedSwedishPlaceIsNotOutside() = runTest {
        assertEquals(
            expected = listOf(false, false),
            actual = listOf(
                isOutsideSweden(locationRepository = FakeLocationRepository()),
                isOutsideSweden(locationRepository = FakeLocationRepository(isAutomatic = false))
            )
        )
    }

    @Test
    fun aPhoneInAnotherCountryIsOutside() = runTest {
        assertEquals(
            expected = listOf(true, true),
            actual = listOf(BERLIN, LONDON).map { location: Location ->
                isOutsideSweden(locationRepository = FakeLocationRepository(deviceLocation = location.asRight()))
            }
        )
    }

    @Test
    fun aPositionThatIsNotKnownIsLeftToTheServer() = runTest {
        assertEquals(
            expected = listOf(false, false),
            actual = listOf(
                isOutsideSweden(locationRepository = FakeLocationRepository(deviceLocation = Failure(errorCode = ErrorCode.LOCATION_PERMISSION_MISSING).asLeft())),
                isOutsideSweden(locationRepository = FakeLocationRepository(deviceLocation = Location(latitude = Double.NaN, longitude = 18.0).asRight()))
            )
        )
    }
}

private suspend fun isOutsideSweden(locationRepository: FakeLocationRepository): Boolean =
    DefaultIsOutsideSwedenTask(getCurrentGeohashTask = DefaultGetCurrentGeohashTask(getChosenPlaceTask = FakeGetChosenPlaceTask(locationRepository = locationRepository)))()

private val BERLIN = Location(
    latitude = 52.52,
    longitude = 13.405
)
private val LONDON = Location(
    latitude = 51.5074,
    longitude = -0.1278
)