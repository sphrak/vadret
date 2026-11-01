package se.vadretapp.vadret.location.local

internal interface DeviceLocationLocalDataSource {

    suspend fun getLastKnownLocation(): LocationEntity
}