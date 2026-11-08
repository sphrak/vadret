package se.vadretapp.vadret.pushalert

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.extension.successOrNull
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.aggregatedfeed.model.ForecastWarningRequest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.followedplace.model.FollowedPlace
import se.vadretapp.vadret.geohash.model.Geohash
import se.vadretapp.vadret.pushalert.model.InstallationToken
import se.vadretapp.vadret.pushalert.usecase.DefaultGetForecastWarningRequestTask
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultGetForecastWarningRequestTaskTest {

    private val pushAlertRepository = FakePushAlertRepository(installationToken = InstallationToken(value = "vadret_token"))
    private val observeFollowedPlacesTask = FakeObserveFollowedPlacesTask()
    private val getForecastWarningRequestTask = DefaultGetForecastWarningRequestTask(
        pushAlertRepository = pushAlertRepository,
        observeFollowedPlacesTask = observeFollowedPlacesTask
    )

    @Test
    fun `without an installation there is nothing to ask vadret api`() =
        runTest {
            pushAlertRepository.installationToken = null
            observeFollowedPlacesTask.followedPlaces.value = listOf(
                followedPlace(
                    latitude = 59.3293,
                    longitude = 18.0686
                )
            ).asRight()

            assertEquals(
                expected = null.asRight<ForecastWarningRequest?>(),
                actual = getForecastWarningRequestTask()
            )
        }

    @Test
    fun `the installation asks about the cell of each followed place in sweden once in an order that hides the chosen place`() =
        runTest {
            observeFollowedPlacesTask.followedPlaces.value = listOf(
                followedPlace(
                    latitude = 59.3293,
                    longitude = 18.0686,
                    isChosen = true
                ),
                followedPlace(
                    latitude = 52.52,
                    longitude = 13.405
                ),
                followedPlace(
                    latitude = 57.7089,
                    longitude = 11.9746
                ),
                followedPlace(
                    latitude = 59.335,
                    longitude = 18.07
                )
            ).asRight()

            assertEquals(
                expected = ForecastWarningRequest(
                    installationToken = "vadret_token",
                    geohashes = listOf(
                        Geohash(value = "u6282"),
                        Geohash(value = "u6sce")
                    )
                ).asRight<ForecastWarningRequest?>(),
                actual = getForecastWarningRequestTask()
            )
        }

    @Test
    fun `the cell the notifications follow is asked about first so it is kept when there are more than five`() =
        runTest {
            pushAlertRepository.syncedCurrentGeohash = Geohash(value = "u6sce")
            observeFollowedPlacesTask.followedPlaces.value = listOf(
                followedPlace(
                    latitude = 55.605,
                    longitude = 13.0038
                ),
                followedPlace(
                    latitude = 57.7089,
                    longitude = 11.9746
                ),
                followedPlace(
                    latitude = 67.8558,
                    longitude = 20.2253
                ),
                followedPlace(
                    latitude = 63.82189,
                    longitude = 20.30458
                ),
                followedPlace(
                    latitude = 59.8586,
                    longitude = 17.6389
                )
            ).asRight()

            val geohashes: List<String>? = getForecastWarningRequestTask().successOrNull?.geohashes?.map { geohash: Geohash -> geohash.value }

            assertEquals(
                expected = listOf(true, 5, true),
                actual = listOf(
                    geohashes.orEmpty().contains(element = "u6sce"),
                    geohashes.orEmpty().size,
                    geohashes == geohashes.orEmpty().sorted()
                )
            )
        }

    @Test
    fun `a cell the notifications follow that cannot be read fails the request`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            val getForecastWarningRequestTask = DefaultGetForecastWarningRequestTask(
                pushAlertRepository = FakePushAlertRepository(
                    installationToken = InstallationToken(value = "vadret_token"),
                    syncedCurrentGeohashFailure = failure
                ),
                observeFollowedPlacesTask = observeFollowedPlacesTask
            )

            assertEquals<Either<Failure, ForecastWarningRequest?>>(
                expected = failure.asLeft(),
                actual = getForecastWarningRequestTask()
            )
        }

    @Test
    fun `followed places that cannot be read fail the request`() =
        runTest {
            val failure = Failure(errorCode = ErrorCode.STORAGE_FAILED)
            observeFollowedPlacesTask.followedPlaces.value = failure.asLeft()

            assertEquals<Either<Failure, ForecastWarningRequest?>>(
                expected = failure.asLeft(),
                actual = getForecastWarningRequestTask()
            )
        }

    private fun followedPlace(
        latitude: Double,
        longitude: Double,
        isChosen: Boolean = false
    ): FollowedPlace =
        FollowedPlace(
            favoriteId = null,
            name = null,
            municipality = null,
            county = null,
            latitude = latitude,
            longitude = longitude,
            isChosen = isChosen
        )
}