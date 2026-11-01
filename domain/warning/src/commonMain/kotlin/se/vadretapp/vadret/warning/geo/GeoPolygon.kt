package se.vadretapp.vadret.warning.geo

import se.vadretapp.vadret.aggregatedfeed.model.GeoPolygon
import se.vadretapp.vadret.aggregatedfeed.model.GeoPosition

private const val ODD = 1
private const val EVEN_ODD_DIVISOR = 2
private const val MINIMUM_RING_SIZE = 3

internal fun GeoPolygon.contains(position: GeoPosition): Boolean = outline.encloses(position = position) && holes.none { hole: List<GeoPosition> -> hole.encloses(position = position) }

private fun List<GeoPosition>.encloses(position: GeoPosition): Boolean {
    if (size < MINIMUM_RING_SIZE) {
        return false
    }
    return (this + first())
        .zipWithNext()
        .count { (start: GeoPosition, end: GeoPosition) ->
            crossesEastOf(
                position = position,
                start = start,
                end = end
            )
        } % EVEN_ODD_DIVISOR == ODD
}

private fun crossesEastOf(
    position: GeoPosition,
    start: GeoPosition,
    end: GeoPosition
): Boolean {
    if ((start.latitude > position.latitude) == (end.latitude > position.latitude)) {
        return false
    }
    val crossingLongitude: Double = start.longitude +
        (position.latitude - start.latitude) / (end.latitude - start.latitude) * (end.longitude - start.longitude)
    return position.longitude < crossingLongitude
}