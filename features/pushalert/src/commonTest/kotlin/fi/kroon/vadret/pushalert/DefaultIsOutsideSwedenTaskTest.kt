package fi.kroon.vadret.pushalert

import fi.kroon.vadret.common.ErrorCode
import fi.kroon.vadret.common.Failure
import fi.kroon.vadret.location.model.Location
import fi.kroon.vadret.pushalert.usecase.DefaultGetCurrentGeohashTask
import fi.kroon.vadret.pushalert.usecase.DefaultIsOutsideSwedenTask
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
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
    DefaultIsOutsideSwedenTask(getCurrentGeohashTask = DefaultGetCurrentGeohashTask(locationRepository = locationRepository))()

private val BERLIN = Location(
    latitude = 52.52,
    longitude = 13.405
)
private val LONDON = Location(
    latitude = 51.5074,
    longitude = -0.1278
)