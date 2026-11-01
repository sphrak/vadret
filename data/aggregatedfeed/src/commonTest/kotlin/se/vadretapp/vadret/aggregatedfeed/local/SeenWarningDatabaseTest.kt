package se.vadretapp.vadret.aggregatedfeed.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SeenWarningDatabaseTest {

    private val testScope = TestScope(context = StandardTestDispatcher())
    private val aggregatedFeedDatabase: AggregatedFeedDatabase = inMemoryAggregatedFeedDatabase(
        coroutineDispatcher = StandardTestDispatcher(scheduler = testScope.testScheduler)
    )
    private val seenWarningDao: SeenWarningDao = aggregatedFeedDatabase.seenWarningDao()
    private val seenWarningLocalDataSource: SeenWarningLocalDataSource = DefaultSeenWarningLocalDataSource(
        aggregatedFeedDatabase = aggregatedFeedDatabase
    )

    @AfterTest
    fun tearDown() {
        aggregatedFeedDatabase.close()
    }

    @Test
    fun `an empty table has no seen warnings`() =
        testScope.runTest {
            assertEquals(expected = emptyList(), actual = seenWarningDao.observeAll().first())
        }

    @Test
    fun `every column is stored and read back`() =
        testScope.runTest {
            val seenWarningEntity = SeenWarningEntity(
                warningIdentifier = "smhi-3376-10720",
                seenAtEpochMillis = 1_729_000_000_123L
            )

            seenWarningLocalDataSource.markSeen(seenWarningEntity = seenWarningEntity)

            assertEquals(
                expected = listOf(seenWarningEntity),
                actual = seenWarningLocalDataSource.observeSeenWarnings().first()
            )
        }

    @Test
    fun `seen warnings are listed latest seen first and by warning identifier when seen together`() =
        testScope.runTest {
            seenWarningDao.insert(
                seenWarningEntity = SeenWarningEntity(
                    warningIdentifier = "b",
                    seenAtEpochMillis = 1L
                )
            )
            seenWarningDao.insert(
                seenWarningEntity = SeenWarningEntity(
                    warningIdentifier = "c",
                    seenAtEpochMillis = 2L
                )
            )
            seenWarningDao.insert(
                seenWarningEntity = SeenWarningEntity(
                    warningIdentifier = "a",
                    seenAtEpochMillis = 1L
                )
            )

            assertEquals(
                expected = listOf("c", "a", "b"),
                actual = seenWarningDao.observeAll().first().map { seenWarningEntity: SeenWarningEntity -> seenWarningEntity.warningIdentifier }
            )
        }

    @Test
    fun `seeing a warning again keeps one row with the latest time`() =
        testScope.runTest {
            seenWarningLocalDataSource.markSeen(
                seenWarningEntity = SeenWarningEntity(
                    warningIdentifier = "a",
                    seenAtEpochMillis = 1L
                )
            )
            seenWarningLocalDataSource.markSeen(
                seenWarningEntity = SeenWarningEntity(
                    warningIdentifier = "a",
                    seenAtEpochMillis = 5L
                )
            )

            assertEquals(
                expected = listOf(
                    SeenWarningEntity(
                        warningIdentifier = "a",
                        seenAtEpochMillis = 5L
                    )
                ),
                actual = seenWarningDao.observeAll().first()
            )
        }
}