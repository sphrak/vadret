package se.vadretapp.vadret.location.exception

internal class LocationProviderDisabledException : Exception("Neither the network nor the GPS location provider is enabled")