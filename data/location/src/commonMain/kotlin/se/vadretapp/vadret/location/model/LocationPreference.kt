package se.vadretapp.vadret.location.model

data class LocationPreference(
    val isAutomatic: Boolean,
    val storedLocation: StoredLocation,
    val followedFavoriteIds: Set<String>
)