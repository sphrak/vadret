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
                    storedLocationEntity = umea
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
}