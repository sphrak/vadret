package se.vadretapp.vadret.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.darwin.NSObject
import kotlin.coroutines.resume

private val authorizedStatuses: Set<CLAuthorizationStatus> = setOf(
    kCLAuthorizationStatusAuthorizedAlways,
    kCLAuthorizationStatusAuthorizedWhenInUse
)

@Composable
actual fun LocationPermissionEffect(
    isRequested: Boolean,
    onResult: (isGranted: Boolean) -> Unit
) {
    val currentOnResult: (Boolean) -> Unit by rememberUpdatedState(newValue = onResult)
    val locationAuthorizationRequester: LocationAuthorizationRequester = remember { LocationAuthorizationRequester() }

    LaunchedEffect(isRequested) {
        if (isRequested) {
            currentOnResult(locationAuthorizationRequester.request())
        }
    }
}

private class LocationAuthorizationRequester :
    NSObject(),
    CLLocationManagerDelegateProtocol {

    private val locationManager: CLLocationManager = CLLocationManager()
    private var pendingContinuation: CancellableContinuation<Boolean>? = null

    init {
        locationManager.delegate = this
    }

    suspend fun request(): Boolean =
        if (locationManager.authorizationStatus == kCLAuthorizationStatusNotDetermined) {
            suspendCancellableCoroutine { continuation: CancellableContinuation<Boolean> ->
                pendingContinuation = continuation
                continuation.invokeOnCancellation { pendingContinuation = null }
                locationManager.requestWhenInUseAuthorization()
            }
        } else {
            locationManager.authorizationStatus in authorizedStatuses
        }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val continuation: CancellableContinuation<Boolean>? = pendingContinuation
        if (continuation != null && manager.authorizationStatus != kCLAuthorizationStatusNotDetermined) {
            pendingContinuation = null
            continuation.resume(value = manager.authorizationStatus in authorizedStatuses)
        }
    }
}