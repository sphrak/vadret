package se.vadretapp.vadret.favorite.local

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.favorite.exception.FavoriteLimitException
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private fun favorite(
    favoriteId: String,
    locality: String,
    municipality: String = "Stockholm",
    createdAt: Long = 0L
): FavoriteEntity =
    FavoriteEntity(
        favoriteId = favoriteId,
        locality = locality,
        municipality = municipality,
        county = "Stockholms län",
        latitude = 59.3293,
        longitude = 18.0686,
        createdAt = createdAt
    )

private const val MAXIMUM_FAVORITES = 5

private suspend fun FavoriteLocalDataSource.add(favoriteEntity: FavoriteEntity): FavoriteEntity =
    addFavorite(
        favoriteEntity = favoriteEntity,
        maximumFavorites = MAXIMUM_FAVORITES
    )

class FavoriteDatabaseTest {

    private val testScope = TestScope(StandardTestDispatcher())
    private val favoriteDatabase: FavoriteDatabase = inMemoryFavoriteDatabase(
        coroutineDispatcher = StandardTestDispatcher(testScope.testScheduler)
    )
    private val favoriteDao: FavoriteDao = favoriteDatabase.favoriteDao()
    private val favoriteLocalDataSource: FavoriteLocalDataSource = DefaultFavoriteLocalDataSource(
        favoriteDatabase = favoriteDatabase
    )

    @AfterTest
    fun tearDown() {
        favoriteDatabase.close()
    }

    @Test
    fun `an empty table has no favorites`() =
        testScope.runTest {
            assertEquals(expected = emptyList(), actual = favoriteDao.observeAll().first())
        }

    @Test
    fun `favorites are listed in the order they were added`() =
        testScope.runTest {
            favoriteDao.insert(favoriteEntity = favorite(favoriteId = "c", locality = "Solna", createdAt = 3L))
            favoriteDao.insert(favoriteEntity = favorite(favoriteId = "a", locality = "Kista", createdAt = 1L))
            favoriteDao.insert(favoriteEntity = favorite(favoriteId = "b", locality = "Bromma", createdAt = 2L))

            assertEquals(
                expected = listOf("a", "b", "c"),
                actual = favoriteDao.observeAll().first().map { favoriteEntity: FavoriteEntity -> favoriteEntity.favoriteId }
            )
        }

    @Test
    fun `favorites added at the same time are listed by id`() =
        testScope.runTest {
            favoriteDao.insert(favoriteEntity = favorite(favoriteId = "b", locality = "Bromma"))
            favoriteDao.insert(favoriteEntity = favorite(favoriteId = "a", locality = "Kista"))

            assertEquals(
                expected = listOf("a", "b"),
                actual = favoriteDao.observeAll().first().map { favoriteEntity: FavoriteEntity -> favoriteEntity.favoriteId }
            )
        }

    @Test
    fun `every column is stored and read back`() =
        testScope.runTest {
            val favoriteEntity = FavoriteEntity(
                favoriteId = "0190a5c3-0000-7000-8000-000000000001",
                locality = "Åre",
                municipality = "Åre",
                county = "Jämtlands län",
                latitude = 63.3990,
                longitude = 13.0815,
                createdAt = 1_729_000_000_000L
            )

            favoriteDao.insert(favoriteEntity = favoriteEntity)

            assertEquals(expected = listOf(favoriteEntity), actual = favoriteDao.observeAll().first())
        }

    @Test
    fun `inserting a stored place fails and keeps the stored row`() =
        testScope.runTest {
            favoriteDao.insert(favoriteEntity = favorite(favoriteId = "first", locality = "Kista", createdAt = 1L))

            assertFailsWith<SQLiteException> {
                favoriteDao.insert(favoriteEntity = favorite(favoriteId = "second", locality = "Kista", createdAt = 2L))
            }
            assertEquals(
                expected = listOf(favorite(favoriteId = "first", locality = "Kista", createdAt = 1L)),
                actual = favoriteDao.observeAll().first()
            )
        }

    @Test
    fun `inserting a stored id fails and keeps the stored row`() =
        testScope.runTest {
            favoriteDao.insert(favoriteEntity = favorite(favoriteId = "first", locality = "Kista"))

            assertFailsWith<SQLiteException> {
                favoriteDao.insert(favoriteEntity = favorite(favoriteId = "first", locality = "Solna"))
            }
            assertEquals(
                expected = listOf(favorite(favoriteId = "first", locality = "Kista")),
                actual = favoriteDao.observeAll().first()
            )
        }

    @Test
    fun `the same locality in another municipality is another place`() =
        testScope.runTest {
            favoriteDao.insert(favoriteEntity = favorite(favoriteId = "a", locality = "Berga", municipality = "Danderyd"))
            favoriteDao.insert(favoriteEntity = favorite(favoriteId = "b", locality = "Berga", municipality = "Linköping"))

            assertEquals(expected = 2, actual = favoriteDao.observeAll().first().size)
        }

    @Test
    fun `a place is found by locality and municipality and county`() =
        testScope.runTest {
            favoriteDao.insert(favoriteEntity = favorite(favoriteId = "a", locality = "Berga", municipality = "Danderyd"))
            favoriteDao.insert(favoriteEntity = favorite(favoriteId = "b", locality = "Berga", municipality = "Linköping"))

            assertEquals(
                expected = "b",
                actual = favoriteDao
                    .getByPlace(
                        locality = "Berga",
                        municipality = "Linköping",
                        county = "Stockholms län"
                    )?.favoriteId
            )
        }

    @Test
    fun `adding a new place returns it`() =
        testScope.runTest {
            val favoriteEntity: FavoriteEntity = favorite(favoriteId = "a", locality = "Kista")

            assertEquals(
                expected = favoriteEntity,
                actual = favoriteLocalDataSource.add(favoriteEntity = favoriteEntity)
            )
        }

    @Test
    fun `adding a stored place returns the stored favorite`() =
        testScope.runTest {
            val storedFavoriteEntity: FavoriteEntity = favorite(favoriteId = "first", locality = "Kista", createdAt = 1L)
            favoriteLocalDataSource.add(favoriteEntity = storedFavoriteEntity)

            val addedFavoriteEntity: FavoriteEntity = favoriteLocalDataSource.add(
                favoriteEntity = favorite(favoriteId = "second", locality = "Kista", createdAt = 2L)
            )

            assertEquals(expected = storedFavoriteEntity, actual = addedFavoriteEntity)
            assertEquals(expected = listOf(storedFavoriteEntity), actual = favoriteDao.observeAll().first())
        }

    @Test
    fun `a new place past the limit is not added`() =
        testScope.runTest {
            favoriteLocalDataSource.addFavorite(
                favoriteEntity = favorite(favoriteId = "a", locality = "Kista"),
                maximumFavorites = 1
            )

            assertFailsWith<FavoriteLimitException> {
                favoriteLocalDataSource.addFavorite(
                    favoriteEntity = favorite(favoriteId = "b", locality = "Solna"),
                    maximumFavorites = 1
                )
            }
            assertEquals(expected = listOf("a"), actual = favoriteDao.observeAll().first().map { favoriteEntity: FavoriteEntity -> favoriteEntity.favoriteId })
        }

    @Test
    fun `removing a favorite keeps the others`() =
        testScope.runTest {
            favoriteLocalDataSource.add(
                favoriteEntity = favorite(
                    favoriteId = "a",
                    locality = "Kista",
                    createdAt = 1L
                )
            )
            favoriteLocalDataSource.add(
                favoriteEntity = favorite(
                    favoriteId = "b",
                    locality = "Solna",
                    createdAt = 2L
                )
            )

            favoriteLocalDataSource.removeFavorite(favoriteId = "a")

            assertEquals(
                expected = listOf(favorite(favoriteId = "b", locality = "Solna", createdAt = 2L)),
                actual = favoriteDao.observeAll().first()
            )
        }

    @Test
    fun `removing an unknown favorite changes nothing`() =
        testScope.runTest {
            favoriteLocalDataSource.add(favoriteEntity = favorite(favoriteId = "a", locality = "Kista"))

            favoriteLocalDataSource.removeFavorite(favoriteId = "unknown")

            assertEquals(
                expected = listOf(favorite(favoriteId = "a", locality = "Kista")),
                actual = favoriteDao.observeAll().first()
            )
        }

    @Test
    fun `a removed place can be added again`() =
        testScope.runTest {
            favoriteLocalDataSource.add(favoriteEntity = favorite(favoriteId = "a", locality = "Kista"))
            favoriteLocalDataSource.removeFavorite(favoriteId = "a")

            favoriteLocalDataSource.add(favoriteEntity = favorite(favoriteId = "b", locality = "Kista"))

            assertEquals(
                expected = listOf(favorite(favoriteId = "b", locality = "Kista")),
                actual = favoriteDao.observeAll().first()
            )
        }
}