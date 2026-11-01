package se.vadretapp.vadret.location.exception

private const val NO_KNOWN_LOCATION = "No enabled location provider knows a location yet"

internal class LocationNotAvailableException(
    message: String = NO_KNOWN_LOCATION
) : Exception(message)