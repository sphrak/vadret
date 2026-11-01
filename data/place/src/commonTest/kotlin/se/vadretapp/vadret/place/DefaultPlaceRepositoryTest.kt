package se.vadretapp.vadret.place

import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.place.local.PlaceEntity
import se.vadretapp.vadret.place.local.PlaceLocalDataSource
import se.vadretapp.vadret.place.model.Place
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakePlaceLocalDataSource(
    private val onSearch: (String, Int) -> List<PlaceEntity>
) : PlaceLocalDataSource {
    override suspend fun search(
        prefix: String,
        limit: Int
    ): List<PlaceEntity> = onSearch(prefix, limit)
}

class DefaultPlaceRepositoryTest {

    @Test
    fun `found places are returned with the prefix and limit asked for`() =
        runTest {
            val asked = mutableListOf<Pair<String, Int>>()
            val placeRepository: PlaceRepository = DefaultPlaceRepository(
                placeLocalDataSource = FakePlaceLocalDataSource { prefix: String, limit: Int ->
                    asked.add(prefix to limit)
                    listOf(
                        PlaceEntity(
                            locality = "Abisko",
                            municipality = "Kiruna",
                            county = "Norrbotten",
                            latitude = 68.349,
                            longitude = 18.83
                        )
                    )
                }
            )

            val result: Either<Failure, List<Place>> = placeRepository.search(
                prefix = "Abi",
                limit = 3
            )

            assertEquals<Either<Failure, List<Place>>>(
                expected = listOf(
                    Place(
                        locality = "Abisko",
                        municipality = "Kiruna",
                        county = "Norrbotten",
                        latitude = 68.349,
                        longitude = 18.83
                    )
                ).asRight(),
                actual = result
            )
            assertEquals(expected = listOf("Abi" to 3), actual = asked)
        }

    @Test
    fun `a list that cannot be read is a file failure`() =
        runTest {
            val exceptions: List<Exception> = listOf(
                IOException("missing"),
                NumberFormatException("north"),
                IllegalArgumentException("no column"),
                IllegalStateException("closed")
            )

            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, List<Place>>> = exceptions.map { exception: Exception ->
                    DefaultPlaceRepository(
                        placeLocalDataSource = FakePlaceLocalDataSource { _, _ -> throw exception }
                    ).search(
                        prefix = "A",
                        limit = 1
                    )
                }

                assertEquals(
                    expected = List(size = exceptions.size) { Failure(errorCode = ErrorCode.FILE_READ_FAILED).asLeft() },
                    actual = results
                )
                assertEquals(
                    expected = List(size = exceptions.size) { LogLevel.WARN },
                    actual = recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }

    @Test
    fun `anything else is unknown`() =
        runTest {
            withRecordedLogs { recordingLogSink ->
                val result: Either<Failure, List<Place>> = DefaultPlaceRepository(
                    placeLocalDataSource = FakePlaceLocalDataSource { _, _ -> throw UnsupportedOperationException("bug") }
                ).search(
                    prefix = "A",
                    limit = 1
                )

                assertEquals(
                    expected = Failure.unknown().asLeft(),
                    actual = result
                )
                assertEquals(expected = LogLevel.ERROR, actual = recordingLogSink.lines.single().level)
            }
        }
}