package se.vadretapp.vadret.location.local

internal data class LocationPreferenceEntity(
    val isAutomatic: Boolean,
    val storedLocationEntity: StoredLocationEntity
)