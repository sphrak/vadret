package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultSelectFavoriteTaskTest {

    private val locationRepository = FakeLocationRepository(isAutomatic = true)
    private val selectFavoriteTask: SelectFavoriteTask = DefaultSelectFavoriteTask(locationRepository = locationRepository)

    @Test
    fun `the favorite becomes the selected location`() =
        runTest {
            assertEquals<Either<Failure, Unit>>(
                expected = Unit.asRight(),
                actual = selectFavoriteTask(favorite = kirunaFavorite)
            )
            assertEquals(
                expected = LocationPreference(
                    isAutomatic = false,
                    storedLocation = StoredLocation(
                        locality = "Kiruna",
                        municipality = "Kiruna",
                        county = "Norrbotten",
                        latitude = 67.8558,
                        longitude = 20.2253
                    )
                ),
                actual = locationRepository.locationPreference.value
            )
        }

    @Test
    fun `a favorite that cannot be selected is a failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            locationRepository.selectStoredLocationResult = failure.asLeft()

            assertEquals<Either<Failure, Unit>>(
                expected = failure.asLeft(),
                actual = selectFavoriteTask(favorite = kirunaFavorite)
            )
        }
}