package se.vadretapp.vadret.weatherforecast.extension

import kotlin.math.floor

private const val PLACE_KEY_SCALE = 100.0
private const val HALF = 0.5

// A saved forecast is found by place to about a kilometre, so the current location finds its forecast after the phone
// has moved a little.
internal fun placeKey(
    latitude: Double,
    longitude: Double
): String = "${latitude.toPlaceKeyPart()},${longitude.toPlaceKeyPart()}"

private fun Double.toPlaceKeyPart(): Long = floor(x = this * PLACE_KEY_SCALE + HALF).toLong()