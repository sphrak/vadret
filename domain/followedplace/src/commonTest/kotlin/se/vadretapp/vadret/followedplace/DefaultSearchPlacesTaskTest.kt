package se.vadretapp.vadret.followedplace

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.place.model.Place
import kotlin.test.Test
import kotlin.test.assertEquals

private const val SUGGESTION_LIMIT = 10

class DefaultSearchPlacesTaskTest {

    private val placeRepository = FakePlaceRepository()
    private val searchPlacesTask: SearchPlacesTask = DefaultSearchPlacesTask(placeRepository = placeRepository)

    @Test
    fun `a search finds the places that start with it`() =
        runTest {
            assertEquals<Either<Failure, List<Place>>>(
                expected = listOf(kiruna).asRight(),
                actual = searchPlacesTask(prefix = "Kir")
            )
            assertEquals(expected = listOf("Kir" to SUGGESTION_LIMIT), actual = placeRepository.searches)
        }

    @Test
    fun `a blank search finds nothing without asking for places`() =
        runTest {
            assertEquals<Either<Failure, List<Place>>>(
                expected = emptyList<Place>().asRight(),
                actual = searchPlacesTask(prefix = " ")
            )
            assertEquals(expected = emptyList(), actual = placeRepository.searches)
        }
}