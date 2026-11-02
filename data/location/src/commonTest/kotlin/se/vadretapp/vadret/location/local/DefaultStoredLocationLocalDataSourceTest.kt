package se.vadretapp.vadret.location.local

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.testing.testPreferencesDataStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val umea = StoredLocationEntity(
    locality = "Umeå",
    municipality = "Umeå",
    county = "Västerbotten",
    latitude = 63.8258,
    longitude = 20.263
)

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultStoredLocationLocalDataSourceTest {

    @Test
    fun `nothing stored follows the device and falls back to Stockholm`() =
        runTest {
            val storedLocationLocalDataSource: StoredLocationLocalDataSource = DefaultStoredLocationLocalDataSource(
                dataStore = testPreferencesDataStore(scope = backgroundScope)
            )

            assertTrue(actual = storedLocationLocalDataSource.isAutomatic())
            assertEquals(
                expected = StoredLocationEntity(
                    locality = "Stockholm",
                    municipality = "Stockholm",
                    county = "Stockholm",
                    latitude = 59.3293,
                    longitude = 18.0686
                ),
                actual = storedLocationLocalDataSource.getStoredLocation()
            )
        }

    @Test
    fun `a selected location reads back and turns automatic off`() =
        runTest {
            val storedLocationLocalDataSource: StoredLocationLocalDataSource = DefaultStoredLocationLocalDataSource(
                dataStore = testPreferencesDataStore(scope = backgroundScope)
            )
            storedLocationLocalDataSource.selectStoredLocation(storedLocationEntity = umea)

            assertEquals(expected = umea, actual = storedLocationLocalDataSource.getStoredLocation())
            assertFalse(actual = storedLocationLocalDataSource.isAutomatic())
        }

    @Test
    fun `turning automatic back on keeps the selected location`() =
        runTest {
            val storedLocationLocalDataSource: StoredLocationLocalDataSource = DefaultStoredLocationLocalDataSource(
                dataStore = testPreferencesDataStore(scope = backgroundScope)
            )
            storedLocationLocalDataSource.selectStoredLocation(storedLocationEntity = umea)

            storedLocationLocalDataSource.setAutomatic(isAutomatic = true)

            assertEquals(
                expected = LocationPreferenceEntity(
                    isAutomatic = true,
                    storedLocationEntity = umea,
                    followedFavoriteIds = emptySet()
                ),
                actual = storedLocationLocalDataSource.observeLocationPreference().first()
            )
        }

    @Test
    fun `observing emits once per distinct preference`() =
        runTest {
            val storedLocationLocalDataSource: StoredLocationLocalDataSource = DefaultStoredLocationLocalDataSource(
                dataStore = testPreferencesDataStore(scope = backgroundScope)
            )
            val emitted = mutableListOf<LocationPreferenceEntity>()
            backgroundScope.launch {
                storedLocationLocalDataSource.observeLocationPreference().toList(destination = emitted)
            }
            testScheduler.advanceUntilIdle()

            storedLocationLocalDataSource.selectStoredLocation(storedLocationEntity = umea)
            testScheduler.advanceUntilIdle()
            storedLocationLocalDataSource.selectStoredLocation(storedLocationEntity = umea)
            testScheduler.advanceUntilIdle()
            storedLocationLocalDataSource.setAutomatic(isAutomatic = true)
            testScheduler.advanceUntilIdle()

            assertEquals(
                expected = listOf(true, false, true),
                actual = emitted.map { locationPreferenceEntity: LocationPreferenceEntity -> locationPreferenceEntity.isAutomatic }
            )
        }

    @Test
    fun `nothing is followed at first and followed favorites read back`() =
        runTest {
            val storedLocationLocalDataSource: StoredLocationLocalDataSource = DefaultStoredLocationLocalDataSource(
                dataStore = testPreferencesDataStore(scope = backgroundScope)
            )
            val nothingFollowed: Set<String> = storedLocationLocalDataSource.observeLocationPreference().first().followedFavoriteIds

            storedLocationLocalDataSource.followFavorite(
                favoriteId = "favorite-1",
                maximumFollowedFavorites = MAXIMUM_FOLLOWED_FAVORITES
            )
            storedLocationLocalDataSource.followFavorite(
                favoriteId = "favorite-2",
                maximumFollowedFavorites = MAXIMUM_FOLLOWED_FAVORITES
            )
            storedLocationLocalDataSource.unfollowFavorite(favoriteId = "favorite-1")

            assertEquals(expected = emptySet(), actual = nothingFollowed)
            assertEquals(
                expected = setOf("favorite-2"),
                actual = storedLocationLocalDataSource.observeLocationPreference().first().followedFavoriteIds
            )
        }

    @Test
    fun `a favorite past the limit is not followed while one already followed stays followed`() =
        runTest {
            val storedLocationLocalDataSource: StoredLocationLocalDataSource = DefaultStoredLocationLocalDataSource(
                dataStore = testPreferencesDataStore(scope = backgroundScope)
            )
            val followed: List<Boolean> = listOf("favorite-1", "favorite-2", "favorite-3", "favorite-1").map { favoriteId: String ->
                storedLocationLocalDataSource.followFavorite(
                    favoriteId = favoriteId,
                    maximumFollowedFavorites = 2
                )
            }

            assertEquals(expected = listOf(true, true, false, true), actual = followed)
            assertEquals(
                expected = setOf("favorite-1", "favorite-2"),
                actual = storedLocationLocalDataSource.observeLocationPreference().first().followedFavoriteIds
            )
        }

    @Test
    fun `the push favorites are adopted once and never over a later choice`() =
        runTest {
            val storedLocationLocalDataSource: StoredLocationLocalDataSource = DefaultStoredLocationLocalDataSource(
                dataStore = testPreferencesDataStore(scope = backgroundScope)
            )

            storedLocationLocalDataSource.adoptFollowedFavoriteIds(favoriteIds = setOf("favorite-1"))
            val adopted: Set<String> = storedLocationLocalDataSource.observeLocationPreference().first().followedFavoriteIds
            storedLocationLocalDataSource.followFavorite(
                favoriteId = "favorite-2",
                maximumFollowedFavorites = MAXIMUM_FOLLOWED_FAVORITES
            )
            storedLocationLocalDataSource.adoptFollowedFavoriteIds(favoriteIds = setOf("favorite-3"))

            assertEquals(expected = setOf("favorite-1"), actual = adopted)
            assertEquals(
                expected = setOf("favorite-1", "favorite-2"),
                actual = storedLocationLocalDataSource.observeLocationPreference().first().followedFavoriteIds
            )
        }

    @Test
    fun `following nothing is a choice the push favorites are not adopted over`() =
        runTest {
            val storedLocationLocalDataSource: StoredLocationLocalDataSource = DefaultStoredLocationLocalDataSource(
                dataStore = testPreferencesDataStore(scope = backgroundScope)
            )

            storedLocationLocalDataSource.followFavorite(
                favoriteId = "favorite-2",
                maximumFollowedFavorites = MAXIMUM_FOLLOWED_FAVORITES
            )
            storedLocationLocalDataSource.unfollowFavorite(favoriteId = "favorite-2")
            storedLocationLocalDataSource.adoptFollowedFavoriteIds(favoriteIds = setOf("favorite-1"))

            assertEquals(expected = emptySet(), actual = storedLocationLocalDataSource.observeLocationPreference().first().followedFavoriteIds)
        }

    @Test
    fun `a selected location keeps what is followed`() =
        runTest {
            val storedLocationLocalDataSource: StoredLocationLocalDataSource = DefaultStoredLocationLocalDataSource(
                dataStore = testPreferencesDataStore(scope = backgroundScope)
            )
            storedLocationLocalDataSource.followFavorite(
                favoriteId = "favorite-1",
                maximumFollowedFavorites = MAXIMUM_FOLLOWED_FAVORITES
            )

            storedLocationLocalDataSource.selectStoredLocation(storedLocationEntity = umea)

            assertEquals(
                expected = LocationPreferenceEntity(
                    isAutomatic = false,
                    storedLocationEntity = umea,
                    followedFavoriteIds = setOf("favorite-1")
                ),
                actual = storedLocationLocalDataSource.observeLocationPreference().first()
            )
        }
}

private const val MAXIMUM_FOLLOWED_FAVORITES = 4