package se.vadretapp.vadret.radar.ui.map

import se.vadretapp.vadret.radar.projection.RADAR_EAST_LONGITUDE
import se.vadretapp.vadret.radar.projection.RADAR_NORTH_LATITUDE
import se.vadretapp.vadret.radar.projection.RADAR_SOUTH_LATITUDE
import se.vadretapp.vadret.radar.projection.RADAR_WEST_LONGITUDE
import se.vadretapp.vadret.radar.projection.WebMercator
import kotlin.math.PI
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow

private const val TILE_SIZE = 512.0
private const val ZOOM_BASE = 2.0
private const val DEGREES_IN_CIRCLE = 360.0
private const val MERCATOR_WORLD_HEIGHT = 2 * PI
private const val HALF = 2.0

private val mercatorNorth: Double = WebMercator.y(latitude = RADAR_NORTH_LATITUDE)
private val mercatorSouth: Double = WebMercator.y(latitude = RADAR_SOUTH_LATITUDE)
private const val LONGITUDE_SPAN: Double = RADAR_EAST_LONGITUDE - RADAR_WEST_LONGITUDE

internal class RadarCameraLimits(
    val minimumZoom: Double,
    val northLatitude: Double,
    val eastLongitude: Double,
    val southLatitude: Double,
    val westLongitude: Double
)

private class CenterRange(
    val lowest: Double,
    val highest: Double
)

internal fun radarCameraLimits(
    viewportWidth: Double,
    viewportHeight: Double,
    obscuredBottom: Double,
    zoom: Double
): RadarCameraLimits {
    val visibleHeight: Double = (viewportHeight - obscuredBottom).coerceAtLeast(minimumValue = viewportHeight / HALF)
    val minimumZoom: Double = max(
        a = log2(x = viewportWidth / (TILE_SIZE * LONGITUDE_SPAN / DEGREES_IN_CIRCLE)),
        b = log2(x = visibleHeight / (TILE_SIZE * (mercatorNorth - mercatorSouth) / MERCATOR_WORLD_HEIGHT))
    )
    val worldSize: Double = TILE_SIZE * ZOOM_BASE.pow(x = max(a = zoom, b = minimumZoom))
    val halfLongitudeSpan: Double = viewportWidth / HALF / worldSize * DEGREES_IN_CIRCLE
    val longitudes: CenterRange = centerRange(
        lowest = RADAR_WEST_LONGITUDE + halfLongitudeSpan,
        highest = RADAR_EAST_LONGITUDE - halfLongitudeSpan
    )
    val mercatorYs: CenterRange = centerRange(
        lowest = mercatorSouth + (visibleHeight - viewportHeight / HALF) / worldSize * MERCATOR_WORLD_HEIGHT,
        highest = mercatorNorth - viewportHeight / HALF / worldSize * MERCATOR_WORLD_HEIGHT
    )
    return RadarCameraLimits(
        minimumZoom = minimumZoom,
        northLatitude = WebMercator.latitude(y = mercatorYs.highest),
        eastLongitude = longitudes.highest,
        southLatitude = WebMercator.latitude(y = mercatorYs.lowest),
        westLongitude = longitudes.lowest
    )
}

private fun centerRange(
    lowest: Double,
    highest: Double
): CenterRange {
    if (lowest <= highest) {
        return CenterRange(
            lowest = lowest,
            highest = highest
        )
    }
    val middle: Double = (lowest + highest) / HALF
    return CenterRange(
        lowest = middle,
        highest = middle
    )
}