package se.vadretapp.vadret.location.local

import android.location.Location
import android.location.LocationManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import se.vadretapp.vadret.location.exception.LocationNotAvailableException
import se.vadretapp.vadret.location.exception.LocationPermissionMissingException
import se.vadretapp.vadret.location.exception.LocationProviderDisabledException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DefaultDeviceLocationLocalDataSourceTest {

    private val testScope = TestScope(StandardTestDispatcher())
    private val locationManager: LocationManager = mockk()
    private val deviceLocationLocalDataSource: DeviceLocationLocalDataSource = DefaultDeviceLocationLocalDataSource(
        locationManager = locationManager,
        coroutineDispatcher = StandardTestDispatcher(testScope.testScheduler)
    )

    private fun location(
        latitude: Double,
        longitude: Double
    ): Location {
        val location: Location = mockk()
        every { location.latitude } returns latitude
        every { location.longitude } returns longitude
        return location
    }

    private fun providers(
        isNetworkEnabled: Boolean,
        isGpsEnabled: Boolean,
        networkLocation: Location?,
        gpsLocation: Location?
    ) {
        every { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns isNetworkEnabled
        every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns isGpsEnabled
        every { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } returns networkLocation
        every { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns gpsLocation
    }

    @Test
    fun `no enabled provider throws`() =
        testScope.runTest {
            providers(
                isNetworkEnabled = false,
                isGpsEnabled = false,
                networkLocation = null,
                gpsLocation = null
            )

            assertFailsWith<LocationProviderDisabledException> { deviceLocationLocalDataSource.getLastKnownLocation() }
        }

    @Test
    fun `the network location is preferred`() =
        testScope.runTest {
            providers(
                isNetworkEnabled = true,
                isGpsEnabled = true,
                networkLocation = location(latitude = 59.0, longitude = 18.0),
                gpsLocation = location(latitude = 60.0, longitude = 19.0)
            )

            assertEquals(
                expected = LocationEntity(latitude = 59.0, longitude = 18.0),
                actual = deviceLocationLocalDataSource.getLastKnownLocation()
            )
            verify(exactly = 0) { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }
        }

    @Test
    fun `gps answers when the network knows no location`() =
        testScope.runTest {
            providers(
                isNetworkEnabled = true,
                isGpsEnabled = true,
                networkLocation = null,
                gpsLocation = location(latitude = 60.0, longitude = 19.0)
            )

            assertEquals(
                expected = LocationEntity(latitude = 60.0, longitude = 19.0),
                actual = deviceLocationLocalDataSource.getLastKnownLocation()
            )
        }

    @Test
    fun `a disabled provider is never asked`() =
        testScope.runTest {
            providers(
                isNetworkEnabled = false,
                isGpsEnabled = true,
                networkLocation = location(latitude = 59.0, longitude = 18.0),
                gpsLocation = location(latitude = 60.0, longitude = 19.0)
            )

            assertEquals(
                expected = LocationEntity(latitude = 60.0, longitude = 19.0),
                actual = deviceLocationLocalDataSource.getLastKnownLocation()
            )
            verify(exactly = 0) { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }
        }

    @Test
    fun `no known location throws`() =
        testScope.runTest {
            providers(
                isNetworkEnabled = true,
                isGpsEnabled = true,
                networkLocation = null,
                gpsLocation = null
            )

            assertFailsWith<LocationNotAvailableException> { deviceLocationLocalDataSource.getLastKnownLocation() }
        }

    @Test
    fun `a revoked permission throws a missing permission`() =
        testScope.runTest {
            every { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } returns true
            every { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } returns false
            every { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } throws SecurityException("revoked")

            assertFailsWith<LocationPermissionMissingException> { deviceLocationLocalDataSource.getLastKnownLocation() }
        }
}