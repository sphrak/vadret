package se.vadretapp.vadret.favorite

import androidx.sqlite.SQLiteException
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.favorite.local.DefaultFavoriteLocalDataSource
import se.vadretapp.vadret.favorite.local.FavoriteDatabase
import se.vadretapp.vadret.favorite.local.FavoriteEntity
import se.vadretapp.vadret.favorite.local.FavoriteLocalDataSource
import se.vadretapp.vadret.favorite.local.inMemoryFavoriteDatabase
import se.vadretapp.vadret.favorite.model.Favorite
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.testing.FixedClock
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.SequentialIdGenerator
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

private const val STARTED_AT_EPOCH_MILLIS = 1_729_000_000_000L

private val kista = Favorite(
    favoriteId = "favorite-1",
    locality = "Kista",
    municipality = "Stockholm",
    county = "Stockholms län",
    latitude = 59.4032,
    longitude = 17.9447
)

private val solna = Favorite(
    favoriteId = "favorite-2",
    locality = "Solna",
    municipality = "Solna",
    county = "Stockholms län",
    latitude = 59.3600,
    longitude = 18.0009
)

private class FailingFavoriteLocalDataSource(
    private val exception: Exception
) : FavoriteLocalDataSource {
    override fun observeFavorites(): Flow<List<FavoriteEntity>> = flow { throw exception }
    override suspend fun addFavorite(favoriteEntity: FavoriteEntity): FavoriteEntity = throw exception
    override suspend fun removeFavorite(favoriteId: String) = throw exception
}

private suspend fun FavoriteRepository.add(favorite: Favorite): Either<Failure, Favorite> =
    addFavorite(
        locality = favorite.locality,
        municipality = favorite.municipality,
        county = favorite.county,
        latitude = favorite.latitude,
        longitude = favorite.longitude
    )

class DefaultFavoriteRepositoryTest {

    private val testScope = TestScope(StandardTestDispatcher())
    private val favoriteDatabase: FavoriteDatabase = inMemoryFavoriteDatabase(
        coroutineDispatcher = StandardTestDispatcher(testScope.testScheduler)
    )
    private val clock = FixedClock(now = Instant.fromEpochMilliseconds(epochMilliseconds = STARTED_AT_EPOCH_MILLIS))
    private val favoriteRepository: FavoriteRepository = favoriteRepository(
        favoriteLocalDataSource = DefaultFavoriteLocalDataSource(favoriteDatabase = favoriteDatabase)
    )

    @AfterTest
    fun tearDown() {
        favoriteDatabase.close()
    }

    private fun favoriteRepository(favoriteLocalDataSource: FavoriteLocalDataSource): FavoriteRepository =
        DefaultFavoriteRepository(
            favoriteLocalDataSource = favoriteLocalDataSource,
            idGenerator = SequentialIdGenerator(ids = listOf("favorite-1", "favorite-2", "favorite-3")),
            clock = clock
        )

    @Test
    fun `adding a place returns it with a generated id`() =
        testScope.runTest {
            assertEquals(
                expected = kista.asRight(),
                actual = favoriteRepository.add(favorite = kista)
            )
        }

    @Test
    fun `adding a stored place again returns the stored favorite`() =
        testScope.runTest {
            favoriteRepository.add(favorite = kista)

            assertEquals(
                expected = kista.asRight(),
                actual = favoriteRepository.add(favorite = kista)
            )
            assertEquals(
                expected = listOf(kista).asRight(),
                actual = favoriteRepository.observeFavorites().first()
            )
        }

    @Test
    fun `favorites are listed in the order they were added`() =
        testScope.runTest {
            favoriteRepository.add(favorite = solna.copy(favoriteId = "favorite-1"))
            clock.now = clock.now - 1.milliseconds
            favoriteRepository.add(favorite = kista.copy(favoriteId = "favorite-2"))

            assertEquals(
                expected = listOf(
                    kista.copy(favoriteId = "favorite-2"),
                    solna.copy(favoriteId = "favorite-1")
                ).asRight(),
                actual = favoriteRepository.observeFavorites().first()
            )
        }

    @Test
    fun `removing a favorite keeps the others`() =
        testScope.runTest {
            favoriteRepository.add(favorite = kista)
            favoriteRepository.add(favorite = solna)

            assertEquals(
                expected = Unit.asRight(),
                actual = favoriteRepository.removeFavorite(favoriteId = kista.favoriteId)
            )
            assertEquals(
                expected = listOf(solna).asRight(),
                actual = favoriteRepository.observeFavorites().first()
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `observing emits the stored favorites and then each change`() =
        testScope.runTest {
            val emitted = mutableListOf<Either<Failure, List<Favorite>>>()
            val job = launch {
                favoriteRepository.observeFavorites().take(count = 4).toList(destination = emitted)
            }
            advanceUntilIdle()

            favoriteRepository.add(favorite = kista)
            advanceUntilIdle()
            favoriteRepository.add(favorite = solna)
            advanceUntilIdle()
            favoriteRepository.removeFavorite(favoriteId = kista.favoriteId)
            advanceUntilIdle()
            job.join()

            assertEquals<List<Either<Failure, List<Favorite>>>>(
                expected = listOf(
                    emptyList<Favorite>().asRight(),
                    listOf(kista).asRight(),
                    listOf(kista, solna).asRight(),
                    listOf(solna).asRight()
                ),
                actual = emitted
            )
        }

    @Test
    fun `storage that cannot be read or written is a storage failure`() =
        testScope.runTest {
            val failingFavoriteRepository: FavoriteRepository = favoriteRepository(
                favoriteLocalDataSource = FailingFavoriteLocalDataSource(exception = SQLiteException("disk full"))
            )

            withRecordedLogs { recordingLogSink ->
                val results: List<Either<Failure, Any>> = listOf(
                    failingFavoriteRepository.add(favorite = kista),
                    failingFavoriteRepository.removeFavorite(favoriteId = kista.favoriteId),
                    failingFavoriteRepository.observeFavorites().toList().single()
                )

                assertEquals(
                    expected = List(size = results.size) { Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft() },
                    actual = results
                )
                assertEquals(
                    expected = List(size = results.size) { "se.vadretapp.vadret.favorite" to LogLevel.WARN },
                    actual = recordingLogSink.lines.map { line: LogLine -> line.tag to line.level }
                )
            }
        }

    @Test
    fun `anything else is unknown and logged as an error`() =
        testScope.runTest {
            val failingFavoriteRepository: FavoriteRepository = favoriteRepository(
                favoriteLocalDataSource = FailingFavoriteLocalDataSource(exception = IllegalStateException("bug"))
            )

            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure.unknown().asLeft(),
                    actual = failingFavoriteRepository.add(favorite = kista)
                )
                assertEquals(expected = LogLevel.ERROR, actual = recordingLogSink.lines.single().level)
            }
        }

    @Test
    fun `cancellation is rethrown`() =
        testScope.runTest {
            val failingFavoriteRepository: FavoriteRepository = favoriteRepository(
                favoriteLocalDataSource = FailingFavoriteLocalDataSource(exception = CancellationException("cancelled"))
            )

            assertFailsWith<CancellationException> {
                failingFavoriteRepository.removeFavorite(favoriteId = kista.favoriteId)
            }
        }
}