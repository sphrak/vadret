package se.vadretapp.vadret.warning.geo

import se.vadretapp.vadret.aggregatedfeed.model.GeoPosition
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot

private const val EARTH_RADIUS_IN_METERS = 6_371_000.0
private const val RADIANS_PER_DEGREE = PI / 180.0
private const val SEGMENT_START = 0.0
private const val SEGMENT_END = 1.0
private const val SINGLE_POINT = 1

private data class PlanePoint(
    val x: Double,
    val y: Double
)

internal fun GeoPosition.distanceInMetersTo(path: List<GeoPosition>): Double {
    val planePoints: List<PlanePoint> = path.map { geoPosition: GeoPosition -> geoPosition.toPlanePoint(origin = this) }
    return if (planePoints.size == SINGLE_POINT) {
        planePoints.single().distanceToOrigin()
    } else {
        planePoints
            .zipWithNext { start: PlanePoint, end: PlanePoint ->
                distanceToOrigin(
                    start = start,
                    end = end
                )
            }
            .minOrNull() ?: Double.POSITIVE_INFINITY
    }
}

private fun GeoPosition.toPlanePoint(origin: GeoPosition): PlanePoint =
    PlanePoint(
        x = (longitude - origin.longitude) * RADIANS_PER_DEGREE * EARTH_RADIUS_IN_METERS * cos(origin.latitude * RADIANS_PER_DEGREE),
        y = (latitude - origin.latitude) * RADIANS_PER_DEGREE * EARTH_RADIUS_IN_METERS
    )

private fun PlanePoint.distanceToOrigin(): Double =
    hypot(
        x = x,
        y = y
    )

private fun distanceToOrigin(
    start: PlanePoint,
    end: PlanePoint
): Double {
    val deltaX: Double = end.x - start.x
    val deltaY: Double = end.y - start.y
    val lengthSquared: Double = deltaX * deltaX + deltaY * deltaY
    val fraction: Double = if (lengthSquared == 0.0) {
        SEGMENT_START
    } else {
        (-(start.x * deltaX + start.y * deltaY) / lengthSquared).coerceIn(
            minimumValue = SEGMENT_START,
            maximumValue = SEGMENT_END
        )
    }
    return PlanePoint(
        x = start.x + fraction * deltaX,
        y = start.y + fraction * deltaY
    ).distanceToOrigin()
}