package se.vadretapp.vadret.radar.ui.overlay

import androidx.compose.ui.geometry.Offset
import se.vadretapp.vadret.location.model.Location
import se.vadretapp.vadret.radar.projection.WebMercator
import kotlin.math.PI
import kotlin.math.pow

private const val TILE_SIZE = 512.0
private const val ZOOM_BASE = 2.0
private const val DEGREES_IN_CIRCLE = 360.0
private const val DEGREES_IN_HALF_CIRCLE = 180.0
private const val MERCATOR_WORLD_HEIGHT = 2 * PI
private const val HALF = 2f

internal class CameraScreenProjection(
    centerLatitude: Double,
    centerLongitude: Double,
    zoom: Double,
    private val viewportWidth: Float,
    private val viewportHeight: Float,
    density: Float
) : MapProjection {

    private val worldSize: Double = TILE_SIZE * ZOOM_BASE.pow(zoom) * density
    private val centerX: Double = mercatorX(longitude = centerLongitude)
    private val centerY: Double = mercatorY(latitude = centerLatitude)

    override fun screenOffset(
        latitude: Double,
        longitude: Double
    ): Offset =
        Offset(
            x = ((mercatorX(longitude = longitude) - centerX) * worldSize).toFloat() + viewportWidth / HALF,
            y = ((mercatorY(latitude = latitude) - centerY) * worldSize).toFloat() + viewportHeight / HALF
        )

    override fun location(screenOffset: Offset): Location {
        val mercatorX: Double = (screenOffset.x - viewportWidth / HALF) / worldSize + centerX
        val mercatorY: Double = (screenOffset.y - viewportHeight / HALF) / worldSize + centerY
        return Location(
            latitude = WebMercator.latitude(y = PI - mercatorY * MERCATOR_WORLD_HEIGHT),
            longitude = mercatorX * DEGREES_IN_CIRCLE - DEGREES_IN_HALF_CIRCLE
        )
    }

    private fun mercatorX(longitude: Double): Double = (longitude + DEGREES_IN_HALF_CIRCLE) / DEGREES_IN_CIRCLE

    private fun mercatorY(latitude: Double): Double = (PI - WebMercator.y(latitude = latitude)) / MERCATOR_WORLD_HEIGHT
}