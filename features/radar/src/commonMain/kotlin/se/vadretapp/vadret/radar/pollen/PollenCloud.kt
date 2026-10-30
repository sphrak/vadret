package se.vadretapp.vadret.radar.pollen

import se.vadretapp.vadret.pollen.model.PollenLevel
import se.vadretapp.vadret.radar.extension.toRadians
import se.vadretapp.vadret.radar.model.PollenMarker
import se.vadretapp.vadret.radar.wind.rasterLatitude
import se.vadretapp.vadret.radar.wind.rasterLongitude
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.roundToInt

private const val KILOMETERS_PER_DEGREE = 111.32
private const val SPREAD_KILOMETERS = 70.0
private const val PIXEL_CENTER = 0.5f
private const val CLOUD_OPACITY = 0.7
private const val CLEAR_AIR_LEVEL = 0.5
private const val FULL_CLOUD_LEVEL = 1.5
private const val SMOOTHSTEP_SQUARE = 3.0
private const val SMOOTHSTEP_CUBE = 2.0
private const val NO_WEIGHT = 1e-9
private const val NO_FRACTION = 0.0
private const val FULL_FRACTION = 1.0
private const val TRANSPARENT = 0
private const val OPAQUE = 255
private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val BLUE_SHIFT = 0
private const val BYTE_MASK = 0xFF
private const val RGB_MASK = 0x00FFFFFF

private val lowestCloudLevel: Int = PollenLevel.LOW.ordinal
private val highestCloudLevel: Int = PollenLevel.VERY_HIGH.ordinal

internal fun pollenCloudPixels(
    pollenMarkers: List<PollenMarker>,
    width: Int,
    height: Int
): IntArray {
    val longitudes = DoubleArray(size = width) { x: Int ->
        rasterLongitude(
            x = x + PIXEL_CENTER,
            width = width
        )
    }
    val pixels = IntArray(size = width * height)
    for (y in 0 until height) {
        val latitude: Double = rasterLatitude(
            y = y + PIXEL_CENTER,
            height = height
        )
        val longitudeKilometers: Double = cos(x = latitude.toRadians()) * KILOMETERS_PER_DEGREE
        for (x in 0 until width) {
            pixels[y * width + x] = cloudArgb(
                pollenMarkers = pollenMarkers,
                latitude = latitude,
                longitude = longitudes[x],
                longitudeKilometers = longitudeKilometers
            )
        }
    }
    return pixels
}

private fun cloudArgb(
    pollenMarkers: List<PollenMarker>,
    latitude: Double,
    longitude: Double,
    longitudeKilometers: Double
): Int {
    var weightSum = 0.0
    var weightedLevel = 0.0
    var strongestWeight = 0.0
    pollenMarkers.forEach { pollenMarker: PollenMarker ->
        val eastKilometers: Double = (longitude - pollenMarker.longitude) * longitudeKilometers
        val northKilometers: Double = (latitude - pollenMarker.latitude) * KILOMETERS_PER_DEGREE
        val weight: Double = exp(x = -(eastKilometers * eastKilometers + northKilometers * northKilometers) / (SPREAD_KILOMETERS * SPREAD_KILOMETERS))
        weightSum += weight
        weightedLevel += weight * pollenMarker.pollenLevel.ordinal
        if (weight > strongestWeight) {
            strongestWeight = weight
        }
    }
    if (weightSum < NO_WEIGHT) {
        return TRANSPARENT
    }
    val level: Double = weightedLevel / weightSum
    val opacity: Double = CLOUD_OPACITY * strongestWeight * smoothstep(value = level)
    val alpha: Int = (opacity * OPAQUE).roundToInt()
    return if (alpha == TRANSPARENT) {
        TRANSPARENT
    } else {
        (alpha shl ALPHA_SHIFT) or (levelRgb(level = level) and RGB_MASK)
    }
}

private fun smoothstep(value: Double): Double {
    val fraction: Double = ((value - CLEAR_AIR_LEVEL) / (FULL_CLOUD_LEVEL - CLEAR_AIR_LEVEL)).coerceIn(
        minimumValue = NO_FRACTION,
        maximumValue = FULL_FRACTION
    )
    return fraction * fraction * (SMOOTHSTEP_SQUARE - SMOOTHSTEP_CUBE * fraction)
}

private fun levelRgb(level: Double): Int {
    val clampedLevel: Double = level.coerceIn(
        minimumValue = lowestCloudLevel.toDouble(),
        maximumValue = highestCloudLevel.toDouble()
    )
    val lowerLevel: Int = floor(x = clampedLevel).toInt()
    val upperLevel: Int = (lowerLevel + 1).coerceAtMost(maximumValue = highestCloudLevel)
    val fraction: Double = clampedLevel - lowerLevel
    val lower: Int = PollenColorScale.argb(pollenLevel = PollenLevel.entries[lowerLevel])
    val upper: Int = PollenColorScale.argb(pollenLevel = PollenLevel.entries[upperLevel])
    val red: Int = channel(
        lower = lower,
        upper = upper,
        shift = RED_SHIFT,
        fraction = fraction
    )
    val green: Int = channel(
        lower = lower,
        upper = upper,
        shift = GREEN_SHIFT,
        fraction = fraction
    )
    val blue: Int = channel(
        lower = lower,
        upper = upper,
        shift = BLUE_SHIFT,
        fraction = fraction
    )
    return (red shl RED_SHIFT) or (green shl GREEN_SHIFT) or (blue shl BLUE_SHIFT)
}

private fun channel(
    lower: Int,
    upper: Int,
    shift: Int,
    fraction: Double
): Int {
    val lowerChannel: Int = (lower shr shift) and BYTE_MASK
    val upperChannel: Int = (upper shr shift) and BYTE_MASK
    return (lowerChannel + (upperChannel - lowerChannel) * fraction).roundToInt()
}