package se.vadretapp.vadret.location

import androidx.datastore.core.IOException
import io.github.sphrak.either.Either
import io.github.sphrak.either.extension.asLeft
import io.github.sphrak.either.extension.asRight
import io.github.sphrak.either.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.common.ErrorCode
import se.vadretapp.vadret.common.Failure
import se.vadretapp.vadret.location.exception.LocationNotAvailableException
import se.vadretapp.vadret.location.exception.LocationPermissionMissingException
import se.vadretapp.vadret.location.exception.LocationProviderDisabledException
import se.vadretapp.vadret.location.local.DeviceLocationLocalDataSource
import se.vadretapp.vadret.location.local.LocationEntity
import se.vadretapp.vadret.location.local.LocationPreferenceEntity
import se.vadretapp.vadret.location.local.StoredLocationEntity
import se.vadretapp.vadret.location.local.StoredLocationLocalDataSource
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.location.model.LocationPreference
import se.vadretapp.vadret.location.model.StoredLocation
import se.vadretapp.vadret.logging.LogLevel
import se.vadretapp.vadret.testing.LogLine
import se.vadretapp.vadret.testing.withRecordedLogs
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeDeviceLocationLocalDataSource(
    private val getLocation: () -> LocationEntity
) : DeviceLocationLocalDataSource {
    override suspend fun getLastKnownLocation(): LocationEntity = getLocation()
}

private val stockholmEntity = StoredLocationEntity(
    locality = "Stockholm",
    municipality = "Stockholm",
    county = "Stockholm",
    latitude = 59.3293,
    longitude = 18.0686
)

private class FakeStoredLocationLocalDataSource : StoredLocationLocalDataSource {

    private val locationPreferenceEntity: MutableStateFlow<LocationPreferenceEntity> = MutableStateFlow(
        value = LocationPreferenceEntity(
            isAutomatic = true,
            storedLocationEntity = stockholmEntity
        )
    )

    override suspend fun isAutomatic(): Boolean = locationPreferenceEntity.value.isAutomatic

    override suspend fun setAutomatic(isAutomatic: Boolean) {
        locationPreferenceEntity.update { entity: LocationPreferenceEntity -> entity.copy(isAutomatic = isAutomatic) }
    }

    override fun observeLocationPreference(): Flow<LocationPreferenceEntity> = locationPreferenceEntity

    override suspend fun selectStoredLocation(storedLocationEntity: StoredLocationEntity) {
        locationPreferenceEntity.update { entity: LocationPreferenceEntity ->
            entity.copy(
                isAutomatic = false,
                storedLocationEntity = storedLocationEntity
            )
        }
    }
}

private class BrokenStoredLocationLocalDataSource : StoredLocationLocalDataSource {
    override suspend fun isAutomatic(): Boolean = throw IOException("corrupt", null)
    override suspend fun setAutomatic(isAutomatic: Boolean) = throw IOException("corrupt", null)
    override fun observeLocationPreference(): Flow<LocationPreferenceEntity> = flow { throw IOException("corrupt", null) }
    override suspend fun selectStoredLocation(storedLocationEntity: StoredLocationEntity) = throw IOException("corrupt", null)
}

private val kiruna = StoredLocation(
    locality = "Kiruna",
    municipality = "Kiruna",
    county = "Norrbotten",
    latitude = 67.8558,
    longitude = 20.2253
)

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultLocationRepositoryTest {

    private fun repository(
        getLocation: () -> LocationEntity = {
            LocationEntity(
                latitude = 57.7,
                longitude = 11.97
            )
        },
        storedLocationLocalDataSource: StoredLocationLocalDataSource = FakeStoredLocationLocalDataSource()
    ): LocationRepository =
        DefaultLocationRepository(
            deviceLocationLocalDataSource = FakeDeviceLocationLocalDataSource(getLocation = getLocation),
            storedLocationLocalDataSource = storedLocationLocalDataSource
        )

    @Test
    fun `the device location is returned`() =
        runTest {
            assertEquals(
                expected = Location(
                    latitude = 57.7,
                    longitude = 11.97
                ).asRight(),
                actual = repository().getDeviceLocation()
            )
        }

    @Test
    fun `device location failures are told apart`() =
        runTest {
            val cases: Map<Exception, ErrorCode> = mapOf(
                LocationProviderDisabledException() to ErrorCode.LOCATION_PROVIDER_DISABLED,
                LocationNotAvailableException() to ErrorCode.LOCATION_NOT_AVAILABLE,
                LocationPermissionMissingException() to ErrorCode.LOCATION_PERMISSION_MISSING
            )

            withRecordedLogs { recordingLogSink ->
                val actual: Map<Exception, Either<Failure, Location>> = cases.keys.associateWith { exception: Exception ->
                    repository(getLocation = { throw exception }).getDeviceLocation()
                }

                assertEquals(
                    expected = cases.mapValues { (_, errorCode: ErrorCode) -> Failure(errorCode = errorCode).asLeft() },
                    actual = actual
                )
                assertEquals(
                    expected = List(size = cases.size) { LogLevel.WARN },
                    actual = recordingLogSink.lines.map { line: LogLine -> line.level }
                )
            }
        }

    @Test
    fun `an unexpected failure is unknown and logged as an error`() =
        runTest {
            withRecordedLogs { recordingLogSink ->
                assertEquals(
                    expected = Failure.unknown().asLeft(),
                    actual = repository(getLocation = { throw IllegalStateException("bug") }).getDeviceLocation()
                )
                assertEquals(expected = LogLevel.ERROR, actual = recordingLogSink.lines.single().level)
            }
        }

    @Test
    fun `the location mode is read and written`() =
        runTest {
            val locationRepository: LocationRepository = repository()

            val before: Either<Failure, Boolean> = locationRepository.isAutomatic()
            locationRepository.setAutomatic(isAutomatic = false)

            assertEquals(expected = true.asRight(), actual = before)
            assertEquals(expected = false.asRight(), actual = locationRepository.isAutomatic())
        }

    @Test
    fun `a selected location reads back and turns automatic off`() =
        runTest {
            val locationRepository: LocationRepository = repository()

            locationRepository.selectStoredLocation(storedLocation = kiruna)

            assertEquals(expected = kiruna.asRight(), actual = locationRepository.observeLocationPreference().first().map { preference: LocationPreference -> preference.storedLocation })
            assertEquals(expected = false.asRight(), actual = locationRepository.isAutomatic())
        }

    @Test
    fun `the observed preference follows each change`() =
        runTest {
            val locationRepository: LocationRepository = repository()
            val emitted = mutableListOf<Either<Failure, LocationPreference>>()
            val job = launch {
                locationRepository.observeLocationPreference().take(count = 2).toList(destination = emitted)
            }
            testScheduler.runCurrent()

            locationRepository.selectStoredLocation(storedLocation = kiruna)
            job.join()

            assertEquals<List<Either<Failure, LocationPreference>>>(
                expected = listOf(
                    LocationPreference(
                        isAutomatic = true,
                        storedLocation = StoredLocation(
                            locality = "Stockholm",
                            municipality = "Stockholm",
                            county = "Stockholm",
                            latitude = 59.3293,
                            longitude = 18.0686
                        )
                    ).asRight(),
                    LocationPreference(
                        isAutomatic = false,
                        storedLocation = kiruna
                    ).asRight()
                ),
                actual = emitted
            )
        }

    @Test
    fun `storage that cannot be read is a storage failure`() =
        runTest {
            val locationRepository: LocationRepository = DefaultLocationRepository(
                deviceLocationLocalDataSource = FakeDeviceLocationLocalDataSource(getLocation = { error("unused") }),
                storedLocationLocalDataSource = BrokenStoredLocationLocalDataSource()
            )

            withRecordedLogs {
                val results: List<Either<Failure, Any>> = listOf(
                    locationRepository.isAutomatic(),
                    locationRepository.setAutomatic(isAutomatic = true),
                    locationRepository.selectStoredLocation(storedLocation = kiruna),
                    locationRepository.observeLocationPreference().toList().single()
                )

                assertEquals(
                    expected = List(size = results.size) { Failure(errorCode = ErrorCode.STORAGE_FAILED).asLeft() },
                    actual = results
                )
            }
        }
}