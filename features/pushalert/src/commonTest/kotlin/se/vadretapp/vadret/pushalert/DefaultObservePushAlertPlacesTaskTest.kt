package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.location.model.StoredLocation
import se.vadretapp.vadret.pushalert.model.PushAlertPlace
import se.vadretapp.vadret.pushalert.usecase.DefaultObservePushAlertPlacesTask
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultObservePushAlertPlacesTaskTest {

    private suspend fun pushAlertPlaces(
        locationRepository: FakeLocationRepository,
        favorites: List<Favorite>
    ): Either<Failure, List<PushAlertPlace>> =
        DefaultObservePushAlertPlacesTask(
            locationRepository = locationRepository,
            favoriteRepository = FakeFavoriteRepository(favorites = favorites)
        )().first()

    private fun List<Favorite>.toSavedPlaces(): List<PushAlertPlace> =
        map { favorite: Favorite ->
            PushAlertPlace.SavedPlace(
                favoriteId = favorite.favoriteId,
                name = favorite.locality
            )
        }

    @Test
    fun `the position comes first and the followed saved places in Sweden follow`() =
        runTest {
            assertEquals<Either<Failure, List<PushAlertPlace>>>(
                expected = (
                    listOf(PushAlertPlace.ChosenPlace(name = null)) +
                        listOf(SOLNA_FAVORITE, GOTEBORG_FAVORITE, UPPSALA_FAVORITE).toSavedPlaces()
                    ).asRight(),
                actual = pushAlertPlaces(
                    locationRepository = FakeLocationRepository(isAutomatic = true),
                    favorites = listOf(
                        SOLNA_FAVORITE,
                        BERLIN_FAVORITE,
                        GOTEBORG_FAVORITE,
                        UPPSALA_FAVORITE,
                        UMEA_FAVORITE,
                        KIRUNA_FAVORITE
                    )
                )
            )
        }

    @Test
    fun `a chosen saved place comes first by its name and only once`() =
        runTest {
            assertEquals<Either<Failure, List<PushAlertPlace>>>(
                expected = (
                    listOf(PushAlertPlace.ChosenPlace(name = VISBY_FAVORITE.locality)) +
                        listOf(SOLNA_FAVORITE, GOTEBORG_FAVORITE).toSavedPlaces()
                    ).asRight(),
                actual = pushAlertPlaces(
                    locationRepository = FakeLocationRepository(
                        isAutomatic = false,
                        storedLocation = StoredLocation(
                            locality = VISBY_FAVORITE.locality,
                            municipality = VISBY_FAVORITE.municipality,
                            county = VISBY_FAVORITE.county,
                            latitude = VISBY_FAVORITE.latitude,
                            longitude = VISBY_FAVORITE.longitude
                        )
                    ),
                    favorites = listOf(SOLNA_FAVORITE, GOTEBORG_FAVORITE, VISBY_FAVORITE)
                )
            )
        }

    @Test
    fun `a location preference that cannot be read is the failure`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)

            assertEquals<Either<Failure, List<PushAlertPlace>>>(
                expected = failure.asLeft(),
                actual = pushAlertPlaces(
                    locationRepository = FakeLocationRepository(failure = failure),
                    favorites = listOf(SOLNA_FAVORITE)
                )
            )
        }
}