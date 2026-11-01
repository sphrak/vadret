package se.vadretapp.vadret.location.local

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import se.vadretapp.vadret.location.exception.LocationNotAvailableException
import se.vadretapp.vadret.location.exception.LocationPermissionMissingException
import se.vadretapp.vadret.location.exception.LocationProviderDisabledException

internal class DefaultDeviceLocationLocalDataSource(
    private val locationManager: LocationManager,
    private val coroutineDispatcher: CoroutineDispatcher
) : DeviceLocationLocalDataSource {

    private val providers: List<String> = listOf(
        LocationManager.NETWORK_PROVIDER,
        LocationManager.GPS_PROVIDER
    )

    override suspend fun getLastKnownLocation(): LocationEntity =
        withContext(context = coroutineDispatcher) {
            val enabledProviders: List<String> = providers.filter { provider: String ->
                locationManager.isProviderEnabled(provider)
            }
            if (enabledProviders.isEmpty()) {
                throw LocationProviderDisabledException()
            }
            val location: Location = enabledProviders.firstNotNullOfOrNull { provider: String ->
                lastKnownLocation(provider = provider)
            } ?: throw LocationNotAvailableException()
            LocationEntity(
                latitude = location.latitude,
                longitude = location.longitude
            )
        }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(provider: String): Location? =
        try {
            locationManager.getLastKnownLocation(provider)
        } catch (exception: SecurityException) {
            throw LocationPermissionMissingException(cause = exception)
        }
}