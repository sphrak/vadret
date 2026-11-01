package se.vadretapp.vadret.location.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.Foundation.NSError
import platform.darwin.NSObject
import se.vadretapp.vadret.location.exception.LocationNotAvailableException
import se.vadretapp.vadret.location.exception.LocationPermissionMissingException
import se.vadretapp.vadret.location.exception.LocationProviderDisabledException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val authorizedStatuses: Set<CLAuthorizationStatus> = setOf(
    kCLAuthorizationStatusAuthorizedAlways,
    kCLAuthorizationStatusAuthorizedWhenInUse
)

internal class DefaultDeviceLocationLocalDataSource(
    private val coroutineDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher
) : DeviceLocationLocalDataSource {

    override suspend fun getLastKnownLocation(): LocationEntity {
        val isLocationServiceEnabled: Boolean = withContext(context = coroutineDispatcher) {
            CLLocationManager.locationServicesEnabled()
        }
        if (!isLocationServiceEnabled) {
            throw LocationProviderDisabledException()
        }
        return withContext(context = mainDispatcher) {
            val locationManager = CLLocationManager()
            if (locationManager.authorizationStatus !in authorizedStatuses) {
                throw LocationPermissionMissingException()
            }
            val location: CLLocation = locationManager.location ?: requestLocation(locationManager = locationManager)
            location.toLocationEntity()
        }
    }

    private suspend fun requestLocation(locationManager: CLLocationManager): CLLocation =
        suspendCancellableCoroutine { continuation: CancellableContinuation<CLLocation> ->
            val delegate = SingleLocationDelegate(continuation = continuation)
            locationManager.delegate = delegate
            continuation.invokeOnCancellation {
                locationManager.stopUpdatingLocation()
                delegate.release(locationManager = locationManager)
            }
            locationManager.requestLocation()
        }
}

private class SingleLocationDelegate(
    private val continuation: CancellableContinuation<CLLocation>
) : NSObject(),
    CLLocationManagerDelegateProtocol {

    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>
    ) {
        val location: CLLocation? = didUpdateLocations.lastOrNull() as? CLLocation
        release(locationManager = manager)
        if (location != null) {
            continuation.resume(value = location)
        } else {
            continuation.resumeWithException(exception = LocationNotAvailableException())
        }
    }

    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError
    ) {
        release(locationManager = manager)
        continuation.resumeWithException(exception = LocationNotAvailableException(message = didFailWithError.localizedDescription))
    }

    fun release(locationManager: CLLocationManager) {
        locationManager.delegate = null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CLLocation.toLocationEntity(): LocationEntity =
    coordinate.useContents {
        LocationEntity(
            latitude = latitude,
            longitude = longitude
        )
    }